package com.litovskiy.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrowthStyleTest {

    @Test
    void findsStyleByKeyIgnoringCaseAndSpaces() {
        assertThat(GrowthStyle.fromKey("tree")).isEqualTo(GrowthStyle.TREE);
        assertThat(GrowthStyle.fromKey(" TREE ")).isEqualTo(GrowthStyle.TREE);
        assertThat(GrowthStyle.fromKey("emotional_intelligence"))
            .isEqualTo(GrowthStyle.EMOTIONAL_INTELLIGENCE);
    }

    @Test
    void returnsNullForUnknownOrMissingKey() {
        assertThat(GrowthStyle.fromKey("unknown")).isNull();
        assertThat(GrowthStyle.fromKey(null)).isNull();
    }

    @Test
    void listsAvailableStyles() {
        assertThat(GrowthStyle.availableStyles())
            .contains("tree")
            .contains("emotional_intelligence");
    }

    @Test
    void convertsValueUsingLargestApplicableUnit() {
        assertThat(GrowthStyle.convertValue(99, GrowthStyle.TREE)).isEqualTo("99.0 см");
        assertThat(GrowthStyle.convertValue(100, GrowthStyle.TREE)).isEqualTo("1.0 м");
        assertThat(GrowthStyle.convertValue(150, GrowthStyle.TREE)).isEqualTo("1.5 м");
        assertThat(GrowthStyle.convertValue(99_999, GrowthStyle.TREE)).isEqualTo("999.99 м");
        assertThat(GrowthStyle.convertValue(100_000, GrowthStyle.TREE)).isEqualTo("1.0 км");
    }

    @Test
    void convertsValueBelowSmallestUnitUsingSmallestUnit() {
        assertThat(GrowthStyle.convertValue(0.5, GrowthStyle.TREE)).isEqualTo("0.5 см");
        assertThat(GrowthStyle.convertValue(-5, GrowthStyle.TREE)).isEqualTo("-5.0 см");
    }

    @Test
    void convertsValueForEmotionalIntelligenceStyle() {
        assertThat(GrowthStyle.convertValue(999, GrowthStyle.EMOTIONAL_INTELLIGENCE))
            .isEqualTo("999.0 женских сил");
        assertThat(GrowthStyle.convertValue(1_500, GrowthStyle.EMOTIONAL_INTELLIGENCE))
            .isEqualTo("1.5К женских сил");
    }
}
