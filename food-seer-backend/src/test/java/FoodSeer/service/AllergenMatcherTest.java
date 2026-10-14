package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for {@link AllergenMatcher}. No Spring context or database.
 */
public class AllergenMatcherTest {

    // normalizeTerm
    @Test
    public void testNormalizeNullAndBlank() {
        assertEquals("", AllergenMatcher.normalizeTerm(null));
        assertEquals("", AllergenMatcher.normalizeTerm(""));
        assertEquals("", AllergenMatcher.normalizeTerm("   "));
        assertEquals("", AllergenMatcher.normalizeTerm("!!!"));
    }

    @Test
    public void testNormalizeCaseAndWhitespace() {
        assertEquals("PEANUT", AllergenMatcher.normalizeTerm("peanut"));
        assertEquals("PEANUT", AllergenMatcher.normalizeTerm("  Peanut  "));
        assertEquals("TREE NUT", AllergenMatcher.normalizeTerm("tree   nut"));
    }

    @Test
    public void testNormalizeRemovesPluralEndings() {
        assertEquals("PEANUT", AllergenMatcher.normalizeTerm("PEANUTS"));
        assertEquals("EGG", AllergenMatcher.normalizeTerm("EGGS"));
        assertEquals("ANCHOVY", AllergenMatcher.normalizeTerm("ANCHOVIES"));
        assertEquals("TREE NUT", AllergenMatcher.normalizeTerm("TREE NUTS"));
    }

    @Test
    public void testNormalizeLeavesWordsThatEndInSButAreNotPlural() {
        assertEquals("BASS", AllergenMatcher.normalizeTerm("BASS"));
        assertEquals("HUMMUS", AllergenMatcher.normalizeTerm("HUMMUS"));
        assertEquals("SOY", AllergenMatcher.normalizeTerm("SOY"));
        assertEquals("GLUTEN", AllergenMatcher.normalizeTerm("GLUTEN"));
    }

    @Test
    public void testNormalizeTreatsHyphenAndUnderscoreAsSpace() {
        assertEquals("TREE NUT", AllergenMatcher.normalizeTerm("TREE-NUTS"));
        assertEquals("TREE NUT", AllergenMatcher.normalizeTerm("tree_nuts"));
    }

    @Test
    public void testNormalizeStripsFillerWords() {
        assertEquals("PEANUT", AllergenMatcher.normalizeTerm("peanut allergy"));
        assertEquals("PEANUT", AllergenMatcher.normalizeTerm("allergic to peanuts"));
        assertEquals("GLUTEN", AllergenMatcher.normalizeTerm("gluten-free"));
        assertEquals("DAIRY", AllergenMatcher.normalizeTerm("non dairy"));
        assertEquals("SHELLFISH", AllergenMatcher.normalizeTerm("No shellfish"));
    }

    @Test
    public void testNormalizeOnlyFillerGivesEmpty() {
        assertEquals("", AllergenMatcher.normalizeTerm("free"));
        assertEquals("", AllergenMatcher.normalizeTerm("allergic to"));
    }

    // parseTerms
    @Test
    public void testParseNullBlankAndEmpty() {
        assertEquals(Collections.emptyList(), AllergenMatcher.parseTerms(null));
        assertEquals(Collections.emptyList(), AllergenMatcher.parseTerms(""));
        assertEquals(Collections.emptyList(), AllergenMatcher.parseTerms("   "));
        assertEquals(Collections.emptyList(), AllergenMatcher.parseTerms(", ; ,"));
    }

    @Test
    public void testParseCommaSeparatedListFromThePreferencesPage() {
        final List<String> terms = AllergenMatcher.parseTerms("PEANUTS, TREE-NUTS, GLUTEN");
        assertEquals(Arrays.asList("PEANUT", "TREE NUT", "GLUTEN"), terms);
    }

    @Test
    public void testParseSingleItem() {
        assertEquals(Arrays.asList("SHELLFISH"), AllergenMatcher.parseTerms("SHELLFISH"));
    }

    @Test
    public void testParseAndOrSemicolonSlashAndColon() {
        assertEquals(Arrays.asList("PEANUT", "EGG"), AllergenMatcher.parseTerms("peanuts and eggs"));
        assertEquals(Arrays.asList("FISH", "SOY"), AllergenMatcher.parseTerms("fish or soy"));
        assertEquals(Arrays.asList("MILK", "WHEAT"), AllergenMatcher.parseTerms("milk; wheat"));
        assertEquals(Arrays.asList("MILK", "DAIRY"), AllergenMatcher.parseTerms("Milk/Dairy"));
        assertEquals(Arrays.asList("PEANUT", "EGG"), AllergenMatcher.parseTerms("Allergies: peanuts, eggs"));
    }

    @Test
    public void testParseRemovesDuplicatesAndKeepsOrder() {
        assertEquals(Arrays.asList("PEANUT", "EGG"), AllergenMatcher.parseTerms("PEANUTS, EGGS, peanut"));
    }

    @Test
    public void testParseDoesNotSplitInsideWordsContainingAndOrOr() {
        assertEquals(Arrays.asList("ORANGE", "CANDY"), AllergenMatcher.parseTerms("orange, candy"));
    }

    // restrictionsAffectedBy: plural, case, spacing
    @Test
    public void testPeanutMatchesPeanutsBothDirections() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("PEANUTS", "peanut"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("peanut", "PEANUTS"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("Peanuts", "Peanuts"));
    }

    @Test
    public void testMatchesWhenAllergenIsOneOfSeveralRestrictions() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("MILK, EGGS, PEANUTS", "peanut"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("MILK, EGGS, PEANUTS", "egg"));
    }

    @Test
    public void testNoMatchWhenAllergenIsNotListed() {
        assertFalse(AllergenMatcher.restrictionsAffectedBy("MILK, EGGS", "peanut"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("SOY", "sesame"));
    }

    // restrictionsAffectedBy: empty inputs
    @Test
    public void testNullOrBlankRestrictionsNeverMatch() {
        assertFalse(AllergenMatcher.restrictionsAffectedBy(null, "peanut"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("", "peanut"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("   ", "peanut"));
    }

    @Test
    public void testNullOrBlankIncidentAllergenNeverMatches() {
        assertFalse(AllergenMatcher.restrictionsAffectedBy("PEANUTS", null));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("PEANUTS", ""));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("PEANUTS", "free"));
    }

    // restrictionsAffectedBy: nuts versus tree nuts versus peanuts
    @Test
    public void testGenericNutsIncidentAffectsPeanutAndTreeNutCustomers() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("PEANUTS", "nuts"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("TREE-NUTS", "nuts"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("ALMOND", "nuts"));
    }

    @Test
    public void testGenericNutsRestrictionIsAffectedByPeanutAndTreeNutIncidents() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("nuts", "peanut"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("nuts", "tree nuts"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("nuts", "walnut"));
    }

    @Test
    public void testTreeNutsAndPeanutsAreSeparate() {
        assertFalse(AllergenMatcher.restrictionsAffectedBy("TREE-NUTS", "peanut"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("PEANUTS", "tree nuts"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("PEANUTS", "almond"));
    }

    @Test
    public void testSpecificTreeNutsAreSeparateFromEachOtherButLinkedToTheGroup() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("ALMOND", "tree nuts"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("TREE-NUTS", "almond"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("ALMOND", "walnut"));
    }

    // restrictionsAffectedBy: fish versus shellfish
    @Test
    public void testFishAndShellfishAreNotConfusedBySubstring() {
        assertFalse(AllergenMatcher.restrictionsAffectedBy("FISH", "shellfish"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("SHELLFISH", "fish"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("FISH", "shrimp"));
    }

    @Test
    public void testShellfishGroupCoversSpecificShellfish() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("SHELLFISH", "shrimp"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("SHELLFISH", "lobster"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("SHELLFISH", "oysters"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("shrimp", "shellfish"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("crab", "shrimp"));
    }

    @Test
    public void testFishGroupCoversSpecificFish() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("FISH", "salmon"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("tuna", "fish"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("tuna", "salmon"));
    }

    // restrictionsAffectedBy: gluten, wheat, dairy, aliases
    @Test
    public void testGlutenAndWheat() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("GLUTEN", "wheat"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("WHEAT", "gluten"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("WHEAT", "barley"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("gluten free", "rye"));
    }

    @Test
    public void testDairyMilkAndLactoseAreTheSameThing() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("MILK", "dairy"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("dairy", "milk"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("LACTOSE", "milk"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("dairy free", "milk"));
    }

    @Test
    public void testSoySynonyms() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("SOY", "soya"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("soybeans", "soy"));
    }

    @Test
    public void testMeatGroup() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("MEAT", "beef"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("POULTRY", "chicken"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("PORK", "beef"));
    }

    @Test
    public void testUnknownTermsMatchOnlyThemselvesAfterNormalizing() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("kiwi", "KIWIS"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("kiwi", "mango"));
        assertTrue(AllergenMatcher.restrictionsAffectedBy("CORN", "corn"));
    }

    @Test
    public void testNaturalLanguageRestrictionText() {
        assertTrue(AllergenMatcher.restrictionsAffectedBy("allergic to peanuts and shellfish", "peanut"));
        assertFalse(AllergenMatcher.restrictionsAffectedBy("vegan", "peanut"));
    }

    // foodDeclaresAllergen
    @Test
    public void testFoodTagsMatchUsingTheSameRules() {
        assertTrue(AllergenMatcher.foodDeclaresAllergen(Arrays.asList("GLUTEN", "PEANUTS"), "peanut"));
        assertTrue(AllergenMatcher.foodDeclaresAllergen(Arrays.asList("TREE-NUTS"), "nuts"));
        assertTrue(AllergenMatcher.foodDeclaresAllergen(Arrays.asList("TREE-NUTS"), "almond"));
        assertFalse(AllergenMatcher.foodDeclaresAllergen(Arrays.asList("TREE-NUTS"), "peanut"));
        assertFalse(AllergenMatcher.foodDeclaresAllergen(Arrays.asList("FISH"), "shellfish"));
    }

    @Test
    public void testFoodWithNoTagsOrNullTagsDeclaresNothing() {
        assertFalse(AllergenMatcher.foodDeclaresAllergen(null, "peanut"));
        assertFalse(AllergenMatcher.foodDeclaresAllergen(Collections.emptyList(), "peanut"));
        assertFalse(AllergenMatcher.foodDeclaresAllergen(Arrays.asList("", "  "), "peanut"));
    }

    @Test
    public void testFoodDeclaresNothingForBlankIncidentAllergen() {
        assertFalse(AllergenMatcher.foodDeclaresAllergen(Arrays.asList("PEANUTS"), null));
        assertFalse(AllergenMatcher.foodDeclaresAllergen(Arrays.asList("PEANUTS"), "  "));
    }
}
