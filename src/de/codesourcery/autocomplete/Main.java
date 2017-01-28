package de.codesourcery.autocomplete;

import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class Main extends JFrame  
{
    private JEditorPane editor;

    public static void main(String[] args) throws InvocationTargetException, InterruptedException 
    {
        SwingUtilities.invokeAndWait( () -> 
        {
            new Main().run();
        } );
    }
    
    private void run() 
    {
        final Set<String> choices = new HashSet<>();
        choices.addAll( Arrays.asList( "test","test1","test2","test123", "blubb" , "bla" ) );
        
        final AutoComplete<String> autoComplete = new AutoComplete<String>();
        
        autoComplete.setCallback( input -> 
        {
            final String lower = input.toLowerCase();
            return choices.stream().filter( c -> c.contains( lower ) ).collect( Collectors.toList() );
        });
        
        editor = new JEditorPane();
        
        autoComplete.attachTo( editor );
        
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        editor.setSize( new Dimension(640,480 ) );
        getContentPane().add( new JScrollPane(editor) );
        pack();
        setLocationRelativeTo( null );
        setVisible( true );
    }     
}