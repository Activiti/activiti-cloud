/*
 * Copyright 2017-2025 Hyland Software, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.bot.builder.BotFrameworkAdapter;
import com.microsoft.bot.connector.Channels;
import com.microsoft.bot.connector.authentication.AuthenticationConfiguration;
import com.microsoft.bot.connector.authentication.MicrosoftAppCredentials;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.Attachment;
import com.microsoft.bot.schema.ChannelAccount;
import com.microsoft.bot.schema.ConversationParameters;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TeamsChatService {

    private final String appId;
    private final String appTenant;
    private final BotFrameworkAdapter adapter;
    private final MicrosoftAppCredentials appCredentials;

    public TeamsChatService(String appId, String appPassword, String appTenant) {
        this.appId = appId;
        this.appTenant = appTenant;

        appCredentials = new MicrosoftAppCredentials(appId, appPassword, appTenant);
        AuthenticationConfiguration authConfig = new AuthenticationConfiguration();

        this.adapter = new BotFrameworkAdapter(appCredentials, authConfig, null, null, null);

        adapter.setOnTurnError((turnContext, exception) -> {
            System.err.println("Exception raised bot: " + exception.getMessage());
            exception.printStackTrace();
            return turnContext.sendActivity("We have a problem!!!").thenApply(result -> null);
        });
    }

    public CompletableFuture<Void> sendSimpleMessage(String teamsUserId, String message) {
        System.out.println("========== SENDING MESSAGE ==========");
        System.out.println("Teams User ID: " + teamsUserId);
        System.out.println("Message: " + message);
        System.out.println("Bot App ID: " + appId);
        System.out.println("Tenant ID: " + appTenant);
        System.out.println("================================================");

        ConversationParameters conversationParams = new ConversationParameters();

        ChannelAccount user = new ChannelAccount();
        user.setId(teamsUserId);
        conversationParams.setMembers(Collections.singletonList(user));

        ChannelAccount bot = new ChannelAccount();
        bot.setId(appId);
        conversationParams.setBot(bot);

        conversationParams.setTenantId(appTenant);

        return adapter
            .createConversation(
                Channels.MSTEAMS,
                "https://smba.trafficmanager.net/amer/",
                appCredentials,
                conversationParams,
                turnContext -> {
                    System.out.println("Conversation Created!");
                    System.out.println("Conversation ID: " + turnContext.getActivity().getConversation().getId());

                    return turnContext
                        .sendActivity(message)
                        .thenApply(result -> {
                            System.out.println("Message sent!");
                            System.out.println("Activity ID: " + result.getId());
                            return null;
                        });
                }
            )
            .exceptionally(throwable -> {
                System.err.println("========== ERROR ==========");
                System.err.println("Type: " + throwable.getClass().getName());
                System.err.println("Message: " + throwable.getMessage());

                Throwable cause = throwable.getCause();
                if (cause != null) {
                    System.err.println("Cause: " + cause.getClass().getName());
                    System.err.println("Error Message: " + cause.getMessage());
                }

                System.err.println("Error:");
                throwable.printStackTrace();
                System.err.println("================================");

                return null;
            });
    }

    public CompletableFuture<Void> sendAdaptiveCard(
        String teamsUserId,
        String title,
        String description,
        Map<String, String> data,
        String jsonDetail
    ) {
        System.out.println("========== SENDING ADAPTIVE CARD ==========");
        System.out.println("Teams User ID: " + teamsUserId);
        System.out.println("Card Title: " + title);
        System.out.println("Bot App ID: " + appId);
        System.out.println("Tenant ID: " + appTenant);
        System.out.println("================================================");

        ConversationParameters conversationParams = new ConversationParameters();

        ChannelAccount user = new ChannelAccount();
        user.setId(teamsUserId);
        conversationParams.setMembers(Collections.singletonList(user));

        ChannelAccount bot = new ChannelAccount();
        bot.setId(appId);
        conversationParams.setBot(bot);

        conversationParams.setTenantId(appTenant);

        return adapter
            .createConversation(
                Channels.MSTEAMS,
                "https://smba.trafficmanager.net/amer/",
                appCredentials,
                conversationParams,
                turnContext -> {
                    System.out.println("Conversation Created!");
                    System.out.println("Conversation ID: " + turnContext.getActivity().getConversation().getId());

                    // Crea l'Adaptive Card
                    //                    Attachment cardAttachment = createAdaptiveCard(title, description, data);

                    Attachment cardAttachment = createComplexAdaptiveCard(title, description, data, jsonDetail);

                    // Crea l'Activity con la card
                    Activity reply = Activity.createMessageActivity();
                    reply.setAttachments(Collections.singletonList(cardAttachment));

                    return turnContext
                        .sendActivity(reply)
                        .thenApply(result -> {
                            System.out.println("Adaptive Card sent!");
                            System.out.println("Activity ID: " + result.getId());
                            return null;
                        });
                }
            )
            .exceptionally(throwable -> {
                System.err.println("========== ERROR ==========");
                System.err.println("Type: " + throwable.getClass().getName());
                System.err.println("Message: " + throwable.getMessage());

                Throwable cause = throwable.getCause();
                if (cause != null) {
                    System.err.println("Cause: " + cause.getClass().getName());
                    System.err.println("Error Message: " + cause.getMessage());
                }

                System.err.println("Error:");
                throwable.printStackTrace();
                System.err.println("================================");

                return null;
            });
    }

    private Attachment createComplexAdaptiveCard(
        String title,
        String description,
        Map<String, String> data,
        String jsonDetails
    ) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode cardContent = mapper.createObjectNode();

        cardContent.put("$schema", "http://adaptivecards.io/schemas/adaptive-card.json");
        cardContent.put("type", "AdaptiveCard");
        cardContent.put("version", "1.5");

        ArrayNode body = mapper.createArrayNode();

        ObjectNode mainContainer = mapper.createObjectNode();
        mainContainer.put("type", "Container");
        mainContainer.put("width", "stretch");

        ArrayNode containerItems = mapper.createArrayNode();

        ObjectNode headerContainer = mapper.createObjectNode();
        headerContainer.put("type", "Container");
        headerContainer.put("style", "emphasis");
        headerContainer.put("width", "stretch");

        ArrayNode headerItems = mapper.createArrayNode();

        ObjectNode titleBlock = mapper.createObjectNode();
        titleBlock.put("type", "TextBlock");
        titleBlock.put("text", title);
        titleBlock.put("size", "Large");
        titleBlock.put("weight", "Bolder");
        titleBlock.put("color", "Accent");
        titleBlock.put("wrap", true);
        headerItems.add(titleBlock);

        headerContainer.set("items", headerItems);
        containerItems.add(headerContainer);

        ObjectNode descriptionBlock = mapper.createObjectNode();
        descriptionBlock.put("type", "TextBlock");
        descriptionBlock.put("text", description);
        descriptionBlock.put("wrap", true);
        descriptionBlock.put("spacing", "Medium");
        containerItems.add(descriptionBlock);

        ObjectNode separator1 = mapper.createObjectNode();
        separator1.put("type", "Container");
        separator1.put("separator", true);
        separator1.put("spacing", "Medium");
        containerItems.add(separator1);

        if (data != null && !data.isEmpty()) {
            ObjectNode dataContainer = mapper.createObjectNode();
            dataContainer.put("type", "Container");
            dataContainer.put("spacing", "Medium");

            ArrayNode dataItems = mapper.createArrayNode();

            for (Map.Entry<String, String> entry : data.entrySet()) {
                ObjectNode labelBlock = mapper.createObjectNode();
                labelBlock.put("type", "TextBlock");
                labelBlock.put("text", entry.getKey() + ":");
                labelBlock.put("weight", "Bolder");
                labelBlock.put("spacing", "Small");
                labelBlock.put("wrap", true);
                dataItems.add(labelBlock);

                ObjectNode valueBlock = mapper.createObjectNode();
                valueBlock.put("type", "TextBlock");
                valueBlock.put("text", entry.getValue());
                valueBlock.put("wrap", true);
                valueBlock.put("spacing", "None");
                valueBlock.put("color", "Default");
                dataItems.add(valueBlock);
            }

            dataContainer.set("items", dataItems);
            containerItems.add(dataContainer);
        }

        ObjectNode separator2 = mapper.createObjectNode();
        separator2.put("type", "Container");
        separator2.put("separator", true);
        separator2.put("spacing", "Medium");
        containerItems.add(separator2);

        ObjectNode jsonContainer = mapper.createObjectNode();
        jsonContainer.put("type", "Container");
        jsonContainer.put("id", "jsonDetailsContainer");
        jsonContainer.put("isVisible", false);
        jsonContainer.put("spacing", "Medium");
        jsonContainer.put("width", "stretch");

        ArrayNode jsonItems = mapper.createArrayNode();

        ObjectNode jsonTitle = mapper.createObjectNode();
        jsonTitle.put("type", "TextBlock");
        jsonTitle.put("text", "📋 Details");
        jsonTitle.put("weight", "Bolder");
        jsonTitle.put("size", "Medium");
        jsonTitle.put("spacing", "Small");
        jsonItems.add(jsonTitle);

        ObjectNode jsonBlock = mapper.createObjectNode();
        jsonBlock.put("type", "TextBlock");
        String formattedJson = formatJsonForDisplay(jsonDetails);
        jsonBlock.put("text", "```json\n" + formattedJson + "\n```");
        jsonBlock.put("wrap", true);
        jsonBlock.put("fontType", "Monospace");
        jsonBlock.put("spacing", "Small");
        jsonBlock.put("maxLines", 0);
        jsonItems.add(jsonBlock);

        jsonContainer.set("items", jsonItems);
        containerItems.add(jsonContainer);

        mainContainer.set("items", containerItems);
        body.add(mainContainer);

        cardContent.set("body", body);

        ArrayNode actions = mapper.createArrayNode();

        ObjectNode toggleAction = mapper.createObjectNode();
        toggleAction.put("type", "Action.ToggleVisibility");
        toggleAction.put("title", "🔍 Event Details");

        ArrayNode targetElements = mapper.createArrayNode();
        ObjectNode target = mapper.createObjectNode();
        target.put("elementId", "jsonDetailsContainer");
        targetElements.add(target);

        toggleAction.set("targetElements", targetElements);
        actions.add(toggleAction);

        ObjectNode openUrlAction = mapper.createObjectNode();
        openUrlAction.put("type", "Action.OpenUrl");
        openUrlAction.put("title", "🌐 Open in Studio Admin");
        openUrlAction.put("url", "https://hxps-alpha.studio.dev.experience.hyland.com/admin/#/processadmin/audit");
        actions.add(openUrlAction);

        cardContent.set("actions", actions);

        Attachment attachment = new Attachment();
        attachment.setContentType("application/vnd.microsoft.card.adaptive");
        attachment.setContent(cardContent);

        return attachment;
    }

    private String formatJsonForDisplay(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(jsonString, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            return jsonString;
        }
    }
}
