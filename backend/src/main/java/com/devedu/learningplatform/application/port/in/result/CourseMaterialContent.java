package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.CourseMaterial;

public record CourseMaterialContent(CourseMaterial material, byte[] content) {
    public CourseMaterialContent { content = content.clone(); }
    @Override public byte[] content() { return content.clone(); }
}
