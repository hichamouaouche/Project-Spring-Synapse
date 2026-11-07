package hiou.hicham.ebankbot.telegram;

import hiou.hicham.ebankbot.agents.EbankAIAgent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true")
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private static final int TELEGRAM_MAX_LENGTH = 4096;

    private final String telegramBotToken;
    private final EbankAIAgent aiAgent;
    private final TelegramClient telegramClient;
    private TelegramBotsLongPollingApplication botsApplication;

    // Token injecte par le constructeur : il est disponible tout de suite pour creer le client
    public TelegramBot(EbankAIAgent aiAgent, @Value("${telegram.token}") String telegramBotToken) {
        this.aiAgent = aiAgent;
        this.telegramBotToken = telegramBotToken;
        this.telegramClient = new OkHttpTelegramClient(telegramBotToken);
    }

    @PostConstruct
    public void registerTelegramBot() {
        try {
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(telegramBotToken, this);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Impossible d'enregistrer le bot Telegram", e);
        }
    }

    @PreDestroy
    public void shutdown() throws Exception {
        if (botsApplication != null) botsApplication.close();
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) return;
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        try {
            List<Media> mediaList = new ArrayList<>();
            String caption = null;

            if (message.hasPhoto()) {
                caption = message.getCaption();
                if (caption == null) caption = "What do you see in this image?";

                // Telegram envoie la meme photo en plusieurs tailles : on garde la plus grande (la derniere)
                List<PhotoSize> photos = message.getPhoto();
                PhotoSize biggest = photos.get(photos.size() - 1);

                File file = telegramClient.execute(new GetFile(biggest.getFileId()));
                byte[] bytes;
                try (InputStream in = telegramClient.downloadFileAsStream(file)) {
                    bytes = in.readAllBytes();
                }
                mediaList.add(Media.builder()
                        .id(biggest.getFileId())
                        .mimeType(MimeTypeUtils.IMAGE_JPEG) // les photos Telegram sont en JPEG
                        .data(new ByteArrayResource(bytes))
                        .build());
            }

            String query = message.getText() != null ? message.getText() : caption;
            if (query == null) {
                // sticker, vocal, document... : rien a envoyer a l'IA
                sendTextMessage(chatId, "Je ne peux traiter que du texte et des images.");
                return;
            }

            UserMessage userMessage = UserMessage.builder()
                    .text(query)
                    .media(mediaList)
                    .build();

            sendTypingAction(chatId);
            String answer = aiAgent.chat(new Prompt(userMessage), "telegram:" + chatId);
            sendTextMessage(chatId, answer);
        } catch (Exception e) {
            log.error("Erreur lors du traitement du message Telegram (chat {})", chatId, e);
            try {
                sendTextMessage(chatId, "Une erreur est survenue, reessaie plus tard.");
            } catch (TelegramApiException ignored) {
                // rien de plus a faire
            }
        }
    }

    private void sendTextMessage(Long chatId, String text) throws TelegramApiException {
        if (text == null || text.isBlank()) text = "(reponse vide)";
        // Telegram refuse les messages de plus de 4096 caracteres : on decoupe
        for (int start = 0; start < text.length(); start += TELEGRAM_MAX_LENGTH) {
            String part = text.substring(start, Math.min(text.length(), start + TELEGRAM_MAX_LENGTH));
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(part)
                    .build());
        }
    }

    private void sendTypingAction(Long chatId) throws TelegramApiException {
        telegramClient.execute(SendChatAction.builder()
                .chatId(chatId)
                .action(ActionType.TYPING.toString())
                .build());
    }
}
