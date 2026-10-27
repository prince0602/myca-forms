package com.mynx.myca500.patient_forms.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BlockTag {
    @JsonProperty("p") P,
    @JsonProperty("h1") H1,
    @JsonProperty("h2") H2,
    @JsonProperty("h3") H3,
    @JsonProperty("blockquote") BLOCKQUOTE,
    @JsonProperty("li") LI,
    @JsonProperty("hr") HR
}
