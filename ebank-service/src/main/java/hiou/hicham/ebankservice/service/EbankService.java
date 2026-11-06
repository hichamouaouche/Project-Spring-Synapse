package hiou.hicham.ebankservice.service;

import hiou.hicham.ebankservice.entities.BankAccount;
import hiou.hicham.ebankservice.feign.CustomerRestClient;
import hiou.hicham.ebankservice.model.Customer;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import hiou.hicham.ebankservice.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service

public class EbankService {
    private BankAccountRepository accountRepository;
    private CustomerRestClient customerRestClient;

    public EbankService(BankAccountRepository accountRepository, CustomerRestClient customerRestClient){
        this.accountRepository = accountRepository;
        this.customerRestClient = customerRestClient;
    }
    @McpTool(description = "Get all bank accounts")
    public List<BankAccount> getAllBankAccounts(){
        return accountRepository.findAll();
    }

    @McpTool(description = "Get bank account by id")
    public BankAccount getBankAccountById(@McpToolParam(description = "The id of the bank account to retrieve") String id){
        BankAccount bankAccount = accountRepository.findById(id)
                .orElseThrow(() ->new RuntimeException("Bank Account not found"));
        bankAccount.setCustomer(
                customerRestClient.getCustomerById(bankAccount.getCustomerId()));
        return bankAccount;
    }
    @McpTool(description = "Save a new bank account. The id is generated automatically.")
    public BankAccount save( @McpToolParam(description = "The bank account to save(balance, type, customerId)") BankAccount bankAccount) {
        try{
            Customer customer = customerRestClient.getCustomerById(bankAccount.getCustomerId());
            bankAccount.setId(UUID.randomUUID().toString());
            bankAccount.setCreationAt(new Date());
            return accountRepository.save(bankAccount);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }


    }
}

