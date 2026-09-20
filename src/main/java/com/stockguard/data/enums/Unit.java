package com.stockguard.data.enums;

import lombok.Getter;

@Getter
public enum Unit {
    // Count
    PIECE("عدد"),
    PACK("بسته"),
    BOX("جعبه"),
    CARTON("کارتن"),
    DOZEN("دوجین"),
    PAIR("جفت"),
    SET("ست"),
    ROLL("رول"),
    SHEET("برگ"),
    BAG("کیسه"),
    BOTTLE("بطری"),
    CAN("قوطی"),
    JAR("شیشه"),
    TRAY("سینی"),
    PALLET("پالت"),
    BUNDLE("دسته"),

    // Weight
    GRAM("گرم"),
    KILOGRAM("کیلوگرم"),
    TON("تن"),
    MITHQAL("مثقال"),
    SEER("سیر"),

    // Volume
    MILLILITER("میلی‌لیتر"),
    LITER("لیتر"),
    CUBIC_METER("متر مکعب"),

    // Length
    MILLIMETER("میلی‌متر"),
    CENTIMETER("سانتی‌متر"),
    METER("متر"),
    KILOMETER("کیلومتر"),

    // Area
    SQUARE_METER("متر مربع");

    private final String faName;

    Unit(String s) {
        this.faName = s;
    }
}
