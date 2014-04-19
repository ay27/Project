/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitman.project.networkmiscellaneous;

import java.util.Iterator;

/**
 *
 * @author 骜
 */
public class SOAPDescriptor {

    public UPnPActionNode action;
    public int argsSum;
    public String deviceType;
    public String serviceType;
    public String actionName;
    public String[] argsNameArray;
    public String[] argsValueArray;
    public String[] argsRelStaVarArray;
    public Direction[] argsDirectionArray;

    public SOAPDescriptor(UPnPActionNode action) {
        Iterator<UPnPArgsNode> argsIterator = action.listArguments.iterator();
        UPnPArgsNode argsNode = null;
        int i = 0;

        this.action = action;
        argsSum = action.listArguments.size();
        deviceType = action.belongToService.belongToDevice.deviceType;
        serviceType = action.belongToService.serviceType;
        actionName = action.name;
        argsNameArray = new String[argsSum];
        argsValueArray = new String[argsSum];
        argsRelStaVarArray = new String[argsSum];
        argsDirectionArray = new Direction[argsSum];
        while (argsIterator.hasNext()) {
            argsNode = argsIterator.next();
            argsNameArray[i] = argsNode.nameArg;
            argsDirectionArray[i] = argsNode.direction;
            argsRelStaVarArray[i] = argsNode.relatedStateVariable;
            argsValueArray[i] = null;
            ++i;
        }
    }
}
