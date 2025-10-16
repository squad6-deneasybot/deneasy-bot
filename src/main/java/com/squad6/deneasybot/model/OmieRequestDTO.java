package com.squad6.deneasybot.model;

import java.util.List;

public record OmieRequestDTO(
        String call,
        String app_key,
        String app_secret,
        List<OmieRequestParamDTO> param
) {}