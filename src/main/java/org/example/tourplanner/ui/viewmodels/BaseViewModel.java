package org.example.tourplanner.ui.viewmodels;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseViewModel {
    private final List<Property<?>> properties = new ArrayList<>();

    protected <T> void registerProperty(Property<T> property) {
        properties.add(property);
    }

    protected <T> void bindProperty(Property<T> source, Property<T> target) {
        target.bind(source);
    }

    protected <T> void addPropertyChangeListener(Property<T> property, ChangeListener<T> listener) {
        property.addListener(listener);
    }

    protected <T> void addValueChangeListener(ObservableValue<T> observable, ChangeListener<T> listener) {
        observable.addListener(listener);
    }

    public void unbindAll() {
        properties.forEach(Property::unbind);
    }
}