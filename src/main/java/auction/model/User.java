package auction.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = "tuser")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String surname;
    private String login;
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;

    public User(Long id) {
        this.id = id;
    }

    public enum Role {
        ADMIN,
        USER
    }
}
