package org.jacorb.test.notification.typed;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2003  Gerald Brose.
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import junit.framework.Assert;
import junit.framework.Test;

import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.servant.TypedProxyPullConsumerImpl;
import org.jacorb.test.notification.NotificationTestCase;
import org.jacorb.test.notification.NotificationTestCaseSetup;
import org.jacorb.test.notification.mocks.NullTaskProcessor;
import org.omg.CORBA.Any;
import org.omg.CORBA.BooleanHolder;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.StringHolder;
import org.omg.CosEventComm.Disconnected;
import org.omg.CosNotification.EventType;
import org.omg.CosNotification.EventTypeHelper;
import org.omg.CosNotification.Property;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.CosNotifyComm.InvalidEventType;
import org.omg.CosTypedNotifyChannelAdmin.TypedProxyPullConsumer;
import org.omg.CosTypedNotifyChannelAdmin.TypedProxyPullConsumerHelper;
import org.omg.CosTypedNotifyComm.TypedPullSupplierPOA;

/**
 * @author Alphonse Bendt
 * @version $Id$
 */
public class TypedProxyPullConsumerImplTest extends NotificationTestCase {

    TypedProxyPullConsumerImpl objectUnderTest_;

    TypedProxyPullConsumer pullConsumer_;

    public void setUp() throws Exception {
        objectUnderTest_ = new TypedProxyPullConsumerImpl(PullCoffeeHelper.id());

        getChannelContext().resolveDependencies(objectUnderTest_);

        objectUnderTest_.preActivate();

        pullConsumer_ = TypedProxyPullConsumerHelper.narrow(objectUnderTest_.activate());
    }

    public TypedProxyPullConsumerImplTest(String name, NotificationTestCaseSetup setup) {
        super(name, setup);
    }


    public void testMyType() {
        assertEquals(ProxyType.PULL_TYPED, pullConsumer_.MyType());
    }


    public void testConnect() throws Exception {
        final MockPullCoffee _coffee = new MockPullCoffee();

        NullTypedPullSupplier _supplier = new NullTypedPullSupplier() {
                public org.omg.CORBA.Object get_typed_supplier() {
                    return _coffee._this(getORB());
                }
            };

        pullConsumer_.connect_typed_pull_supplier(_supplier._this(getORB()));
    }


    public void testTryOperationsAreInvoked() throws Exception {
        final MockPullCoffee _coffee = new MockPullCoffee();

        _coffee.try_drinking_coffee_expect = 1;
        _coffee.try_cancel_coffee_expect = 1;

        NullTypedPullSupplier _supplier = new NullTypedPullSupplier() {
                public org.omg.CORBA.Object get_typed_supplier() {
                    return _coffee._this(getORB());
                }
            };

        pullConsumer_.connect_typed_pull_supplier(_supplier._this(getORB()));

        objectUnderTest_.runPullMessage();

        _coffee.verify();
    }


    public void testFormat() throws Exception {
        final MockPullCoffee _coffee = new MockPullCoffee() {
                public boolean try_drinking_coffee(StringHolder name, IntHolder minutes) {
                    super.try_drinking_coffee(name, minutes);

                    name.value = "jacorb";
                    minutes.value = 20;

                    return true;
                }
            };

        _coffee.try_drinking_coffee_expect = 1;
        _coffee.try_cancel_coffee_expect = 1;

        NullTypedPullSupplier _supplier = new NullTypedPullSupplier() {
                public org.omg.CORBA.Object get_typed_supplier() {
                    return _coffee._this(getORB());
                }
            };

        pullConsumer_.connect_typed_pull_supplier(_supplier._this(getORB()));

        objectUnderTest_.setTaskProcessor(new NullTaskProcessor() {
                public void processMessage(Message mesg) {
                    try {
                        Property[] _props = mesg.toTypedEvent();

                        assertEquals(3, _props.length);

                        assertEquals("event_type", _props[0].name);
                        EventType et = EventTypeHelper.extract(_props[0].value);
                        assertEquals(PullCoffeeHelper.id(), et.domain_name);

                        assertEquals("::org::jacorb::test::notification::typed::PullCoffee::drinking_coffee",
                                     et.type_name);

                        assertEquals("jacorb", _props[1].value.extract_string());
                        assertEquals(20, _props[2].value.extract_long());
                    } catch (Exception e) {
                        fail();
                    }
                }
            });

        objectUnderTest_.runPullMessage();
    }


    public static Test suite() throws Exception {
        return NotificationTestCase.suite(TypedProxyPullConsumerImplTest.class);
    }
}

class NullTypedPullSupplier extends TypedPullSupplierPOA {

    public org.omg.CORBA.Object get_typed_supplier() {
        return null;
    }

    public Any pull() {
        throw new NO_IMPLEMENT();
    }

    public Any try_pull(BooleanHolder booleanHolder) throws Disconnected {
        throw new NO_IMPLEMENT();
    }

    public void disconnect_pull_supplier() {}

    public void subscription_change(EventType[] eventTypeArray,
                                    EventType[] eventTypeArray1) throws InvalidEventType {}
}


class MockPullCoffee extends PullCoffeePOA {

    int drinking_coffee_called;
    int drinking_coffee_expect;

    int try_drinking_coffee_called;
    int try_drinking_coffee_expect;

    int cancel_coffee_called;
    int cancel_coffee_expect;

    int try_cancel_coffee_called;
    int try_cancel_coffee_expect;

    public void drinking_coffee(StringHolder stringHolder, IntHolder intHolder) {
        drinking_coffee_called++;
    }


    public boolean try_drinking_coffee(StringHolder stringHolder, IntHolder intHolder){
        try_drinking_coffee_called++;

        stringHolder.value = "";
        intHolder.value = 0;

        return false;
    }

    public void cancel_coffee(StringHolder stringHolder) {
        cancel_coffee_called++;
    }

    public boolean try_cancel_coffee(StringHolder stringHolder) {
        try_cancel_coffee_called++;

        stringHolder.value = "";

        return false;
    }


    public void verify() {
        Assert.assertEquals(cancel_coffee_expect, cancel_coffee_called);
        Assert.assertEquals(try_cancel_coffee_expect, try_cancel_coffee_called);
        Assert.assertEquals(drinking_coffee_expect, drinking_coffee_called);
        Assert.assertEquals(try_drinking_coffee_expect, try_drinking_coffee_called);
    }
}
