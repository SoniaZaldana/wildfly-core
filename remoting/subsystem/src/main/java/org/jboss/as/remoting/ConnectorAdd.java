/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.remoting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.remoting.CommonAttributes.SECURITY_REALM;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.xnio.OptionMap;

/**
 * Add a connector to a remoting container.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class ConnectorAdd extends AbstractAddStepHandler {

    static final ConnectorAdd INSTANCE = new ConnectorAdd();

    private ConnectorAdd() {
        // TODO pass in the ADs and remove populateModel
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException{
        ConnectorResource.SOCKET_BINDING.validateAndSet(operation, model);
        ConnectorResource.AUTHENTICATION_PROVIDER.validateAndSet(operation, model);
        ConnectorResource.SECURITY_REALM.validateAndSet(operation, model);
        ConnectorCommon.SASL_PROTOCOL.validateAndSet(operation, model);
        ConnectorCommon.SERVER_NAME.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String connectorName = address.getLastElement().getValue();
        ServiceName tmpDirPath = ServiceName.JBOSS.append("server", "path", "jboss.controller.temp.dir");
        final String securityRealm = model.hasDefined(SECURITY_REALM) ? model.require(SECURITY_REALM).asString() : null;
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        RemotingServices.installSecurityServices(context, context.getServiceTarget(), connectorName, null, null, securityRealm, null, tmpDirPath);
        launchServices(context, connectorName, fullModel);
    }

    void launchServices(OperationContext context, String connectorName, ModelNode fullModel) throws OperationFailedException {
        OptionMap optionMap = ConnectorUtils.getFullOptions(context, fullModel);

        final ServiceTarget target = context.getServiceTarget();

        final String socketName = ConnectorResource.SOCKET_BINDING.resolveModelAttribute(context, fullModel).asString();
        final ServiceName socketBindingName = context.getCapabilityServiceName(ConnectorResource.SOCKET_CAPABILITY_NAME, socketName, SocketBinding.class);
        RemotingServices.installConnectorServicesForSocketBinding(target, RemotingServices.SUBSYSTEM_ENDPOINT, connectorName, socketBindingName, optionMap);

    }
}
