package io.quarkiverse.zeebe.runtime.health;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.ClientStatusException;
import io.camunda.zeebe.client.api.response.Topology;
import io.quarkus.runtime.configuration.ConfigUtils;

@Liveness
public class ZeebeTopologyHealthCheck implements HealthCheck {

    private static final String PROP_ZEEBE_ENABLED = "camunda.client.zeebe.enabled";

    @Inject
    ZeebeClient client;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Zeebe topology health check");
        if (!ConfigUtils.isPropertyPresent(PROP_ZEEBE_ENABLED) ||
                ConfigProvider.getConfig().getConfigValue(PROP_ZEEBE_ENABLED).getValue().equals("true")) {
            try {
                Topology topology = client.newTopologyRequest().send().join();
                if (topology.getClusterSize() > 0) {
                    return builder.up().build();
                }
                return builder.down().withData("reason", "No brokers found").build();
            } catch (ClientStatusException e) {
                return builder.down()
                        .withData("reason", e.getMessage())
                        .withData("code", e.getStatusCode().value())
                        .build();
            } catch (Exception ex) {
                return builder.down().withData("reason", ex.getMessage()).build();
            }
        } else {
            return builder.up().build();
        }
    }
}
