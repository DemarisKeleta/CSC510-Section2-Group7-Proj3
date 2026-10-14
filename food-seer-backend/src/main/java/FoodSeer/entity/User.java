package FoodSeer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import FoodSeer.dto.RegisterRequestDto;

@Entity
@Table(name = "users")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue ( strategy = GenerationType.IDENTITY )
    private long   id;

    @Column ( nullable = false, unique = true )
    private String username;

    @Column ( nullable = false, unique = true )
    private String email;

    @Column ( nullable = false )
    private String password;

    @Builder.Default
    @Column ( nullable = false, length = 20 )
    private String role = "ROLE_CUSTOMER";

    @Column ( length = 50 )
    private String costPreference;

    @Column ( length = 500 )
    private String dietaryRestrictions;

    /**
     * Customer's self-set daily calorie goal. Nullable: a customer who
     * never set one has no goal, distinct from a goal of 0.
     */
    // Integer (not int) so "never set" is representable as null; Lombok @Data above generates the getter/setter
    private Integer dailyCalorieGoal;

    public User ( final User o, final String pass ) {
        this.id = o.getId();
        this.username = o.getUsername();
        this.email = o.getEmail();
        this.password = pass;
        this.role = o.getRole();
        this.costPreference = o.getCostPreference();
        this.dietaryRestrictions = o.getDietaryRestrictions();
        // carry the calorie goal through this copy-constructor, same as the other preference fields above
        this.dailyCalorieGoal = o.getDailyCalorieGoal();
    }

    public User ( final RegisterRequestDto o, final String pass, final String role ) {
        this.username = o.username();
        this.email = o.email();
        this.password = pass;
        this.role = role;
    }

}
