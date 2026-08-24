package com.devedu.learningplatform.application.port.in;

import java.util.List;

public interface ListExternalAuthProvidersUseCase {
    List<String> listEnabledProviders();
}
