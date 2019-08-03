package org.tensorflow.lite.examples.classification.misc;

import java.util.ArrayList;

public class ChoiceDetail extends SimpleDetail {
    private ArrayList<String> choices;

    public ChoiceDetail(String key, String description, String value){
        super(key, description, value);
        choices = new ArrayList<String>();
    }

    public ArrayList<String> getChoices(){
        return choices;
    }

    public void addChoice(String value){
        choices.add(value);
    }
}
