package hiou.hicham.ebankservice.entities;

import hiou.hicham.ebankservice.model.Customer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.*;

import java.util.Date;
@Entity
@Getter
@Setter
@AllArgsConstructor @NoArgsConstructor @Builder
public class BankAccount {
    @Id
    private String id;
    private Date creationAt;
    private String balance;
    private String type;
    private long customerId;
    @Transient
    private Customer customer;
}
