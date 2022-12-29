package by.smirnov.telegrambot.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@Entity(name = "users")
@Table(schema = "tg-bot")
public class User {

    @Id
    @Column(name="chat_id")
    private Long chatId;
    @Column(name="first_name")
    private String firstName;
    @Column(name="last_name")
    private String lastName;
    @Column(name="user_name")
    private String userName;
    @Column(name="registered_at")
    private Timestamp registeredAt;
}