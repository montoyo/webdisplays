/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Criterion implements ICriterionTrigger<Criterion.Instance> {

    public static class Instance extends AbstractCriterionInstance {

        public Instance(ResourceLocation id) {
            super(id);
        }

    }

    private final ResourceLocation id;
    private final HashMap<PlayerAdvancements, ArrayList<Listener<Instance>>> map = new HashMap<>();

    public Criterion(@Nonnull String name) {
        id = new ResourceLocation("webdisplays", name);
    }

    @Override
    @Nonnull
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public void addListener(@Nonnull PlayerAdvancements adv, @Nonnull Listener<Instance> l) {
        map.computeIfAbsent(adv, k -> new ArrayList<>()).add(l);
    }

    @Override
    public void removeListener(@Nonnull PlayerAdvancements adv, @Nonnull Listener<Instance> l) {
        map.computeIfPresent(adv, (k, v) -> {
            v.remove(l);
            return v.isEmpty() ? null : v;
        });
    }

    @Override
    public void removeAllListeners(@Nonnull PlayerAdvancements adv) {
        map.remove(adv);
    }

    @Override
    @Nonnull
    public Instance deserializeInstance(@Nonnull JsonObject json, @Nonnull JsonDeserializationContext ctx) {
        return new Instance(id);
    }

    public void trigger(PlayerAdvancements ply) {
        ArrayList<Listener<Instance>> listeners = map.get(ply);

        if(listeners != null) {
            Listener[] copy = listeners.toArray(new Listener[0]); //We need to make a copy, otherwise we get a ConcurrentModificationException
            Arrays.stream(copy).forEach(l -> l.grantCriterion(ply));
        }
    }

}
