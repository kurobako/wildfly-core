/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.jboss.as.controller.security;

import static org.jboss.as.controller.logging.ControllerLogger.ROOT_LOGGER;

import org.jboss.logging.Logger;
import org.wildfly.common.Assert;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * Client class used to talk to {@link CredentialStore} to obtain secret value (credential) from it.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public class CredentialStoreClient {

    private final CredentialStore credentialStore;
    private final String name;
    private final String alias;
    private final Class<? extends Credential> type;

    /**
     * Constructor to create instance of {@link CredentialStoreClient}.
     * @param credentialStore actual {@link CredentialStore} used by this client
     * @param name of credential store
     * @param alias of credential in the store
     * @param type of credential denoted by this alias
     */
    public CredentialStoreClient(final CredentialStore credentialStore, final String name, final String alias, final Class<? extends Credential> type) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("credentialStore", credentialStore);
        this.name = name;
        this.alias = alias;
        this.type = type;
        this.credentialStore = credentialStore;
    }

    /**
     * Constructor to create instance of {@link CredentialStoreClient}.
     * @param credentialStore actual {@link CredentialStore} used by this client
     * @param name of credential store
     * @param alias of credential in the store
     */
    public CredentialStoreClient(final CredentialStore credentialStore, final String name, final String alias) {
        this(credentialStore, name, alias, (Class<Credential>) null);
    }

    /**
     * Constructor to create instance of {@link CredentialStoreClient}.
     * @param credentialStore actual {@link CredentialStore} used by this client
     * @param name of credential store
     * @param alias of credential in the store
     * @param className of credential denoted by this alias
     * @throws ClassNotFoundException when credential reference holding credential type which cannot be resolved using current providers
     */
    public CredentialStoreClient(final CredentialStore credentialStore, final String name, final String alias, final String className) throws ClassNotFoundException {
        this(credentialStore, name, alias, toClass(className, credentialStore.getClass().getClassLoader()));
    }

    private static Class<? extends Credential> toClass(final String className, final ClassLoader classLoader) throws ClassNotFoundException {
        return (Class<Credential>) Class.forName(className, true, classLoader);
    }

    /**
     * Get the secret in form of clear text.
     * @return secret as clear text or {@code null} when destroyed.
     */
    public char[] getSecret() {
        PasswordCredential passwordCredential = (PasswordCredential) getCredential();
        if (passwordCredential != null) {
            if (passwordCredential.getPassword() instanceof ClearPassword) {
                return ((ClearPassword) passwordCredential.getPassword()).getPassword();
            } else {
                ROOT_LOGGER.log(Logger.Level.DEBUG, ROOT_LOGGER.unsupportedCredentialType(name,
                        passwordCredential.getPassword().getClass().getName()));
            }
        }
        return null;
    }

    /**
     * Get the {@link Credential} instance from the credential store.
     * @return Credential instance of proper credential type specified if not available {@code null}
     */
    public Credential getCredential() {
        if (alias == null) {
            return null;
        }
        try {
            if (type != null) {
                return credentialStore.retrieve(alias, type);
            } else {
                return credentialStore.retrieve(alias, PasswordCredential.class);
            }
        } catch (CredentialStoreException e) {
            ROOT_LOGGER.log(Logger.Level.INFO, e);
        }
        return null;
    }

    /**
     * Retrieve associated {@link CredentialStore}
     * This method should perform check whether the {@link CredentialStore} can be returned to caller.
     *
     * @return associated {@link CredentialStore}
     */
    public CredentialStore getCredentialStore() {
        // TODO: check caller if possible
        return credentialStore;
    }

    /**
     * Get alias used by this credential store client to fetch credential
     * @return alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Get type used by this credential store client to fetch credential
     * @return type of credential
     */
    public Class<? extends Credential> getType() {
        return type;
    }

}
