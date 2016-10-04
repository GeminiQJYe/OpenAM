/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package com.iplanet.dpro.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

import org.forgerock.openam.session.service.SessionAccessManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionID;

public class AuthenticationSessionStoreTest {

    private SessionAccessManager mockAccessManager;
    private InternalSession mockSession;
    private SessionID mockSessionID;
    private AuthenticationSessionStore store;

    @BeforeMethod
    public void setup() {
        mockAccessManager = mock(SessionAccessManager.class);

        mockSession = mock(InternalSession.class);
        mockSessionID = mock(SessionID.class);

        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockSession.getTimeLeft()).willReturn(1000L);

        store = new AuthenticationSessionStore(mockAccessManager);
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldNotAllowSessionToBeAddedTwice() {
        store.addSession(mockSession);
        store.addSession(mockSession);
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldNotAllowAlreadyPersistedSessionToBeAdded() {
        given(mockSession.isStored()).willReturn(true);
        store.addSession(mockSession);
    }

    @Test
    public void shouldStoreSession() {
        store.addSession(mockSession);
        assertThat(store.getSession(mockSessionID)).isEqualTo(mockSession);
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldNotPromoteSessionIfNotStored() {
        store.promoteSession(mockSessionID);
    }

    @Test
    public void shouldUseSessionAccessManagerForPromotion() {
        store.addSession(mockSession);
        store.promoteSession(mockSessionID);
        verify(mockAccessManager).putInternalSessionIntoInternalSessionCache(eq(mockSession));
    }

    @Test
    public void shouldReturnNullForNullSessionID() {
        assertThat(store.getSession(null)).isNull();
    }

    @Test
    public void shouldTimeoutSessionAfterTimeout() {
        given(mockSession.getTimeLeft()).willReturn(0L);
        store.addSession(mockSession);
        assertThat(store.getSession(mockSessionID)).isNull();
    }

    @Test
    public void shouldCancelSessionsAddedToAuthenticationSessionStore() {
        store.addSession(mockSession);
        verify(mockSession).cancel();
    }

    @Test
    public void shouldScheduleSessionsPromotedFromTheStore() {
        store.addSession(mockSession);
        store.promoteSession(mockSessionID);
        verify(mockSession).reschedule();
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldThrowExceptionIfNullSessionIsRemoved() {
        store.removeSession(null);
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldThrowExceptionIfSessionNotInStore() {
        store.removeSession(mockSessionID);
    }
}