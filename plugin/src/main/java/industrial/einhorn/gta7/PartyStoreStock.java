package industrial.einhorn.gta7;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Party Store stock generation (VS2 -- founder real-time, 2026-08-10: "can we increase the
// chance that party stores sell tnt by like 90000%?" -> "also extensive potion selection" ->
// "not all party stores have same potions" -> "also add food" -> "party stores always have some
// kind of food" -> "even if its magically created"). Party Stores previously had NO custom trade
// offers at all -- PartyStoreListener's own doc comment says "real trading (vanilla,
// unmodified)," meaning whatever a Villager's naturally-rolled profession happened to offer,
// which never includes TNT (no vanilla trade table anywhere sells it) and rarely offers useful
// potions. "90000%" isn't a literal percentage of a real baseline -- the real baseline was 0%,
// TNT was never a real offer -- read as "make it a sure thing," implemented here as a GUARANTEED
// trade slot rather than a weighted roll. Applied once, at designation time
// (PartyStoreListener's own sneak+right-click branch), via Villager#setRecipes -- vanilla entity
// NBT persists the result across restarts the same as any other villager trade list, so no extra
// save-file plumbing is needed in PartyStoreManager for this.
final class PartyStoreStock {

    private PartyStoreStock() {}

    private static final Random RANDOM = new Random();

    // "extensive potion selection" -- a real pool, not 2-3 tokens. Beneficial/utility potions
    // only (no HARMING/POISON/WEAKNESS) -- this is a shop selling TO players, not against them.
    private static final PotionType[] POTION_POOL = {
        PotionType.HEALING,
        PotionType.STRONG_HEALING,
        PotionType.FIRE_RESISTANCE,
        PotionType.LONG_FIRE_RESISTANCE,
        PotionType.SWIFTNESS,
        PotionType.STRONG_SWIFTNESS,
        PotionType.STRENGTH,
        PotionType.LONG_STRENGTH,
        PotionType.NIGHT_VISION,
        PotionType.LONG_NIGHT_VISION,
        PotionType.INVISIBILITY,
        PotionType.LONG_INVISIBILITY,
        PotionType.REGENERATION,
        PotionType.STRONG_REGENERATION,
        PotionType.WATER_BREATHING,
        PotionType.LONG_WATER_BREATHING,
        PotionType.LEAPING,
        PotionType.SLOW_FALLING,
        PotionType.LUCK,
    };

    // "not all party stores have same potions" -- each store gets this many, randomly, not the
    // whole pool -- real store-to-store variety, not a copy-pasted universal shop.
    private static final int POTIONS_PER_STORE = 4;

    // "party stores always have some kind of food ... even if its magically created" -- don't
    // worry about in-world food-sourcing realism (a Party Store isn't a farm), just always stock
    // exactly one, randomly picked so stores vary here too.
    private static final Material[] FOOD_POOL = {
        Material.BREAD,
        Material.COOKED_BEEF,
        Material.COOKED_PORKCHOP,
        Material.COOKED_CHICKEN,
        Material.BAKED_POTATO,
        Material.PUMPKIN_PIE,
        Material.COOKIE,
        Material.GOLDEN_CARROT,
    };

    static void apply(Villager villager) {
        List<MerchantRecipe> recipes = new ArrayList<>();

        // Guaranteed TNT offer -- see this class's own doc comment on "90000%."
        recipes.add(simpleTrade(new ItemStack(Material.TNT, 1), Material.EMERALD, 3));

        // Guaranteed food offer -- one, randomly chosen, always present.
        Material food = FOOD_POOL[RANDOM.nextInt(FOOD_POOL.length)];
        recipes.add(simpleTrade(new ItemStack(food, 4), Material.EMERALD, 2));

        // A random subset of the potion pool, distinct per store.
        List<PotionType> shuffled = new ArrayList<>(List.of(POTION_POOL));
        Collections.shuffle(shuffled, RANDOM);
        for (int i = 0; i < POTIONS_PER_STORE && i < shuffled.size(); i++) {
            recipes.add(potionTrade(shuffled.get(i)));
        }

        villager.setRecipes(recipes);
    }

    private static MerchantRecipe simpleTrade(ItemStack result, Material cost, int costAmount) {
        // uses=0, maxUses=99 -- plentiful, not just present, matching the "90000%" spirit of
        // abundance rather than a single-use novelty offer.
        MerchantRecipe recipe = new MerchantRecipe(result, 0, 99, true);
        recipe.addIngredient(new ItemStack(cost, costAmount));
        return recipe;
    }

    private static MerchantRecipe potionTrade(PotionType type) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        MerchantRecipe recipe = new MerchantRecipe(potion, 0, 99, true);
        recipe.addIngredient(new ItemStack(Material.EMERALD, 4));
        return recipe;
    }
}
