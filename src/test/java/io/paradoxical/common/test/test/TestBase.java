package io.paradoxical.common.test.test;

import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

public class TestBase {

    protected PodamFactory fixture = new PodamFactoryImpl();

    public TestBase() {

    }
}