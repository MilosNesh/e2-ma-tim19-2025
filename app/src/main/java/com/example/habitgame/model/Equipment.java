package com.example.habitgame.model;

public class Equipment {
    private String name;
    private String type;
    private String effect;
    private double effectPercentage;
    private int duration;
    private boolean isActivated;
    private String image;

    private double price;
    public Equipment() {
    }

    public Equipment(String name, String type, String effect, double effectPercentage, int duration, boolean isActivated, String image, double price) {
        this.name = name;
        this.type = type;
        this.effect = effect;
        this.effectPercentage = effectPercentage;
        this.duration = duration;
        this.isActivated = isActivated;
        this.image = image;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
        this.effect = effect;
    }

    public double getEffectPercentage() {
        return effectPercentage;
    }

    public void setEffectPercentage(double effectPercentage) {
        this.effectPercentage = effectPercentage;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean activated) {
        isActivated = activated;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
