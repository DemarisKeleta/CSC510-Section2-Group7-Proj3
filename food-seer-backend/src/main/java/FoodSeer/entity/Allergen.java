package FoodSeer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A shared, site-wide allergen name. Seeded with the original fixed list
 * at startup, and grows over time as customers add custom allergies that
 * aren't already in it - once added, the new name becomes a real option
 * for every customer (and for staff tagging food items), not just the
 * customer who typed it in.
 */
@Entity
@Table(name = "allergens")
@Data
@NoArgsConstructor
public class Allergen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    public Allergen(final String name) {
        this.name = name;
    }
}
