package com.frozenheart.backend.core.entity.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PushPreferencesTest {

    @Test
    void comparesIndependentCopiesAndDetectsNestedEdits() {
        var original = PushPreferences.createDefault();
        var editedCategory = PushPreferences.createDefault();
        var editedQuietHour = PushPreferences.createDefault();

        assertEquals(original, editedCategory);
        assertEquals(original.hashCode(), editedQuietHour.hashCode());

        editedCategory.getCategories().get(PushNotificationType.ACADEMIC).setEnabled(false);
        editedQuietHour.getQuietHour().setEnabled(true);

        assertNotEquals(original, editedCategory);
        assertNotEquals(original, editedQuietHour);
    }
}
