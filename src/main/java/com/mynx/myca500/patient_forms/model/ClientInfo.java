package com.mynx.myca500.patient_forms.model;

public record ClientInfo(String ip, String userAgent) {

    public static ClientInfo empty() {
        return new ClientInfo(null, null);
    }
}