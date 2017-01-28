package de.codesourcery.autocomplete;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class AutoComplete<T> 
{
    private final Point mousePosition = new Point();

    private final JList<String> list = new JList<>();

    private final AutoCompleteBuffer buffer = new AutoCompleteBuffer();
    private boolean autoCompleteActive;
    private long activationTimestamp;
    private Window popup;

    private JEditorPane editor;

    private final MyKeyDispatcher keyInterceptor = new MyKeyDispatcher();

    private final class MyKeyDispatcher implements KeyEventDispatcher 
    {
        private final List<Character> keyTypedEventsToSwallow = new ArrayList<>();
        private final List<Integer> keyReleasedEventsToSwallow = new ArrayList<>();

        private boolean shouldBeSwallowed(KeyEvent e) 
        {
            if ( isKeyTyped(e) && ! keyTypedEventsToSwallow.isEmpty() )
            {
                final int idx = keyTypedEventsToSwallow.indexOf( e.getKeyChar() );
                if ( idx != -1 ) 
                {
                    keyTypedEventsToSwallow.remove( idx );   
                    return true;
                }
            }
            if ( isKeyRelease( e ) && ! keyReleasedEventsToSwallow.isEmpty() ) {
                final int idx = keyReleasedEventsToSwallow.indexOf( e.getKeyCode() );
                if ( idx != -1 ) 
                {
                    keyReleasedEventsToSwallow.remove( idx );   
                    return true;
                }
            }
            return false;
        }
        
        private void swallowKeyReleased(KeyEvent ev) 
        {
            System.out.println("Going to swallow KEY_RELEASED for "+ev);
            this.keyReleasedEventsToSwallow.add( ev.getKeyCode() );
        }        
        
        private void swallowKeyTyped(KeyEvent ev) 
        {
            System.out.println("Going to swallow KEY_TYPED for "+ev);
            this.keyTypedEventsToSwallow.add( ev.getKeyChar() );
        }
        
        public void reset() 
        {
            keyTypedEventsToSwallow.clear();
            keyReleasedEventsToSwallow.clear();
        }

        public boolean dispatchKeyEvent (KeyEvent e) 
        {
            System.out.flush();
            
            if ( shouldBeSwallowed( e ) ) 
            {
                System.out.println("["+autoCompleteActive+"] Swallowed: "+e);
                e.consume();
                return true;
            }
            
            boolean handled = false;
            if ( autoCompleteActive ) 
            {
                if ( isKeyTyped( e ) ) {
                    
                    switch ( e.getKeyChar() ) 
                    {
                        case ' ':
                        case 0x10:
                        case 0x0a:
                            e.consume();
                            handled = true;                            
                            break;
                        default:
                    }
                } else if ( isKeyPress( e ) ) 
                {
                    switch( e.getKeyCode() ) 
                    {
                        case KeyEvent.VK_DOWN:
                        case KeyEvent.VK_UP:
                        case KeyEvent.VK_ENTER: 
                        case KeyEvent.VK_SPACE: 
                            e.consume();
                            handled = true;
                            break;
                        default:
                    }
                } 
                else if ( isKeyRelease( e ) ) 
                {
                    final int selectedIndex = list.getSelectedIndex();
                    if ( e.getKeyCode() == KeyEvent.VK_DOWN) 
                    {
                        e.consume();     
                        handled = true;
                        if ( (selectedIndex+1) < list.getModel().getSize() ) 
                        {
                            list.setSelectedIndex( selectedIndex+1 );
                        }
                    } 
                    else if ( e.getKeyCode() == KeyEvent.VK_UP ) 
                    {
                        e.consume();   
                        handled = true;
                        if ( selectedIndex > 0 ) 
                        {
                            list.setSelectedIndex( selectedIndex-1 );
                        }
                    }
                    else if ( e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE ) 
                    {
                        e.consume();
                        handled = true;
                        final boolean isSpace = e.getKeyCode() == KeyEvent.VK_SPACE;
                        final boolean ctrlDown = isCtrlDown(e);
                        final long timeSinceActivationMillis = timeSinceActivationMillis( e );
                        final boolean spaceTrigger =  ( ! ctrlDown && isSpace && timeSinceActivationMillis > 200 );
                        System.out.println("is_space: "+isSpace+" , ctrl_down: "+ctrlDown+" , delay: "+timeSinceActivationMillis);
                        if ( e.getKeyCode() == KeyEvent.VK_ENTER || spaceTrigger  ) 
                        {
                            System.out.println("Closing popup, selected index: "+list.getSelectedIndex());
                            final String selectedValue = list.getSelectedValue();
                            hidePopup( selectedValue );
                        }
                        if ( spaceTrigger ) { // only space key will get a KEY_TYPED event...
                            swallowKeyTyped(e);
                        }
                    }                    
                }
            }
            if ( handled ) {
                System.out.println("===> CONSUMED: "+e);
                System.out.flush();
            } else {
                System.out.println("["+autoCompleteActive+"] Key event: "+e);                
            }
            return handled;
        }
    };

    private IAutoCompleteCallback<T> callback = input -> 
    {
        return Collections.emptyList();
    };

    public interface IAutoCompleteCallback<X> 
    {
        public List<X> getChoices(String input);
    }

    private final class AutoCompleteBuffer 
    {
        private final StringBuilder buffer = new StringBuilder();

        private int autoCompleteCaretStartPosition;

        public void reset(String initialInput,int autoCompleteCaretStartPosition) 
        {
            this.autoCompleteCaretStartPosition = autoCompleteCaretStartPosition;
            buffer.setLength( 0 );
            insert(initialInput,autoCompleteCaretStartPosition);
        }

        private int localOffset(int input) 
        {
            return input - autoCompleteCaretStartPosition;
        }

        public void insert(String s,int editorCaretPosition) 
        {
            System.out.println("INSERT @ "+editorCaretPosition+": >"+s+"<");
            buffer.insert( localOffset(editorCaretPosition) , s ); 
            refreshChoicesList();
        }

        public void remove(int len,int offset) 
        {
            System.out.println("Remove @ "+offset+": "+len+" characters");
            final int start = localOffset(offset);
            final int end = start + len;
            buffer.delete( start , end ); 
            refreshChoicesList();
        }

        public int length() {
            return buffer.length();
        }

        public String getValue() {
            return buffer.toString();
        }
    }

    private final KeyAdapter keyListener = new KeyAdapter() 
    {
        public void keyPressed(java.awt.event.KeyEvent e) 
        {
            if ( e.getKeyCode() == KeyEvent.VK_SPACE && isCtrlDown(e) )
            {
                try 
                {
                    final StringBuilder buffer = new StringBuilder();
                    int current = editor.getCaretPosition()-1;
                    while ( current >= 0 && ! Character.isWhitespace( editor.getText().charAt( current ) ) ) 
                    {
                        buffer.insert(0 , editor.getText().charAt( current ) );
                        current -= 1;
                    }
                    final Rectangle rect = editor.modelToView( current+1 );           
                    System.out.println("*** show popup ***");
                    showPopup( rect , e.getWhen() , buffer.toString() , current+1 );
                } 
                catch (BadLocationException e1) 
                {
                    e1.printStackTrace();
                }
            }
        };
    };

    private final MouseMotionListener mouseListener = new MouseAdapter() {

        public void mouseMoved(java.awt.event.MouseEvent e) 
        {
            mousePosition.setLocation( e.getPoint() );
        }
    };

    private DocumentListener documentListener = new DocumentListener() 
    {
        @Override
        public void removeUpdate(DocumentEvent e) 
        {
            if ( autoCompleteActive ) {
                buffer.remove( e.getLength() , e.getOffset() );
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) 
        {
            final int len = e.getLength();
            final int offset = e.getOffset();
            final String text = editor.getText().substring( offset , offset+len );            
            System.out.println("insertUpdate(): >"+text+"<");
            if ( autoCompleteActive ) 
            {
                buffer.insert( text , offset );
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) 
        {
        }
    };

    public void showPopup(Rectangle rect,long activationTimestamp, String initialContent, int autoCompleteStartCaretPosition) 
    {
        if ( ! autoCompleteActive ) 
        {
            autoCompleteActive = true;
            this.activationTimestamp = activationTimestamp;

            final Frame frame = (Frame) editor.getTopLevelAncestor();
            if ( frame == null ) {
                throw new IllegalStateException("Editor has not been added to a frame ?");
            }
            popup = new Window( frame );
            popup.setFocusable( false );
            popup.setAutoRequestFocus(false);
            list.setSize( new Dimension(100,300));
            popup.add( new JScrollPane( list) );
            popup.setSize( new Dimension(100,300));

            Point loc = editor.getLocationOnScreen();
            loc.x += rect.x;
            loc.y += rect.y+15;
            popup.setLocation( loc );
            popup.setVisible( true );

            keyInterceptor.reset();
            buffer.reset( initialContent , autoCompleteStartCaretPosition);
        }
    }

    public void hidePopup(String userChoice) 
    {
        System.out.println("hidePopup(): >"+userChoice+"<");
        if ( autoCompleteActive ) 
        {
            autoCompleteActive = false;
            popup.setVisible( false );
            popup.dispose();

            if ( userChoice != null && userChoice.length() > 0 ) 
            {
                final Document doc = editor.getDocument();
                try {
                    System.out.println("Removing "+buffer.length()+" characters at offset "+buffer.autoCompleteCaretStartPosition);
                    doc.remove( buffer.autoCompleteCaretStartPosition , buffer.length() );
                    System.out.println("Inserting >"+userChoice+" at offset "+buffer.autoCompleteCaretStartPosition);
                    doc.insertString(buffer.autoCompleteCaretStartPosition,userChoice,null);
                }
                catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void attachTo(JEditorPane editor) 
    {
        if ( ! SwingUtilities.isEventDispatchThread() ) {
            throw new IllegalStateException("This method must be called from the EDT");
        }
        this.editor = editor;
        editor.getDocument().addDocumentListener( documentListener );
        editor.addKeyListener( keyListener );
        editor.addMouseMotionListener( mouseListener );

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher( keyInterceptor );
    }

    public void detach() 
    {
        hidePopup( null );
        editor.getDocument().removeDocumentListener( documentListener );
        editor.removeKeyListener( keyListener );
        editor.removeMouseMotionListener( mouseListener );
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher( keyInterceptor );
    }

    public void setCallback(IAutoCompleteCallback<T> callback) 
    {
        this.callback = callback;
    }

    private void refreshChoicesList() 
    {
        System.out.println("Auto-complete: >"+buffer.getValue()+"<");        
        list.setListData( callback.getChoices( buffer.getValue() ).toArray( new String[0] ) );
        list.setSelectedIndex(0);
    }

    private static boolean isKeyPress(KeyEvent e) {
        return e.getID() == KeyEvent.KEY_PRESSED;
    }
    
    private static boolean isKeyRelease(KeyEvent e) {
        return e.getID() == KeyEvent.KEY_RELEASED;
    }

    private static boolean isKeyTyped(KeyEvent e) {
        return e.getID() == KeyEvent.KEY_TYPED;
    }

    private static boolean isCtrlDown(KeyEvent e) {
        return (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;
    }

    private long timeSinceActivationMillis(KeyEvent e) {
        return e.getWhen() - activationTimestamp;
    }
}