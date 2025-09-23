package com.chris.culsi.catalog

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DS_NAME = "culsi_item_catalog"
// Declare this ONCE in the entire project.
val Context.itemCatalogDataStore by preferencesDataStore(name = DS_NAME)

enum class CatalogCategory { BREAKFAST, LUNCH_DINNER, SAUCES_MISE }

object CatalogDefaults {
    val BREAKFAST = listOf(
        "Bacon","Breakfast Potatoes","Breakfast Sandwich","Breakfast Sausage",
        "Chicken Country Gravy","Chicken Sausage","Cut Fruit","French Toast",
        "Fruit Parfaits","Fried Eggs","Gluten Free Waffle Batter","Hashbrowns",
        "Kolaches","Pico","Refried Beans","Salsa","Scrambled eggs",
        "Shredded Cheese","Shredded Hashbrowns","Waffle Batter"
    )

    // Broad umbrellas + common staples
    val LUNCH_DINNER = listOf(
        "Barley","Beef","Beans","Bread","Chicken","Corn","Eggs",
        "Fish","Green Vegetables","Noodles","Pasta","Pork",
        "Potatoes","Quinoa","Rice","Root Vegetables","Salad",
        "Seafood","Soup","Starch","Tofu","Tortillas","Vegetables"
    )

    val SAUCES_MISE = listOf(
        "Aioli","Balsamic","BBQ Sauce","Buffalo","Caesar","Chipotle Sauce",
        "Cream","Dijon","Gravy","Guac","Honey Mustard","House Sauce",
        "Ketchup","Mayo","Mustard","Pesto","Pickles","Pico","Queso",
        "Ranch","Relish","Salsa","Shredded Cheese","Sour Cream","Vinaigrette"
    )
}

class ItemCatalogRepository(private val context: Context) {
    private val breakfastKey = stringSetPreferencesKey("catalog_breakfast_user")
    private val lunchDinnerKey = stringSetPreferencesKey("catalog_lunch_dinner_user")
    private val saucesKey = stringSetPreferencesKey("catalog_sauces_mise_user")
    private val lastUserKey = stringPreferencesKey("last_logged_by")

    private fun keyFor(cat: CatalogCategory): Preferences.Key<Set<String>> = when (cat) {
        CatalogCategory.BREAKFAST     -> breakfastKey
        CatalogCategory.LUNCH_DINNER  -> lunchDinnerKey
        CatalogCategory.SAUCES_MISE   -> saucesKey
    }

    fun mergedSortedItems(cat: CatalogCategory): Flow<List<String>> {
        val defaults = when (cat) {
            CatalogCategory.BREAKFAST    -> CatalogDefaults.BREAKFAST
            CatalogCategory.LUNCH_DINNER -> CatalogDefaults.LUNCH_DINNER
            CatalogCategory.SAUCES_MISE  -> CatalogDefaults.SAUCES_MISE
        }
        val key = keyFor(cat)
        return context.itemCatalogDataStore.data.map { prefs: Preferences ->
            val user: List<String> = (prefs[key]?.toList()).orEmpty()
            (defaults + user).distinct().sortedBy { it.lowercase() }
        }
    }

    suspend fun addCustom(cat: CatalogCategory, item: String) {
        val clean = item.trim()
        if (clean.isEmpty()) return
        val key = keyFor(cat)
        context.itemCatalogDataStore.edit { prefs ->
            val cur = prefs[key]?.toMutableSet() ?: mutableSetOf()
            cur.add(clean)
            prefs[key] = cur
        }
    }

    fun lastLoggedBy(): Flow<String> =
        context.itemCatalogDataStore.data.map { prefs: Preferences ->
            prefs[lastUserKey] ?: ""
        }

    suspend fun setLastLoggedBy(name: String) {
        context.itemCatalogDataStore.edit { it[lastUserKey] = name.trim() }
    }
}
