package hiou.hicham.ebankbot.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hiou.hicham.ebankbot.agents.EbankAIAgent;

@RestController
public class EbankChatbotController {
    private EbankAIAgent ebankAIAgent;

    public EbankChatbotController(EbankAIAgent ebankAIAgent) {
        this.ebankAIAgent = ebankAIAgent;
    }




    @GetMapping(value = "/chat", produces = "application/json")
    public String chat(@RequestParam(name = "query", defaultValue = "Bonjour") String query) {
        return ebankAIAgent.chat(query);
    }
}