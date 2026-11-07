package hiou.hicham.ebankbot.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class EbankAIAgent {
    private ChatClient chatClient;

    public EbankAIAgent(ChatClient.Builder chatClient,
                        ChatMemory chatMemory, ToolCallbackProvider tools) {
        this.chatClient = chatClient
                .defaultSystem("""
                    Vous un assistant qui se charge de repondre aux questions
                    de l'utilisateur a propos des clients et des comptes bancaires, en fonction du context fourni.
                    Si aucun contexte n'est fourni, repond avec JE NE SAIS PAS.
                    """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors(a -> a.param(ChatMemory.CONVERSATION_ID, "default"))
                .defaultTools(tools)
                .build();
    }


    public String chat(String query) {
        return chatClient.prompt(query).call().content();
    }

    public String chat(Prompt prompt) {
        return chatClient.prompt(prompt).call().content();
    }

    // conversationId : une memoire separee par utilisateur (ex: "telegram:123456")
    public String chat(Prompt prompt, String conversationId) {
        return chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
