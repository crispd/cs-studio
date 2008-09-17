package org.csstudio.debugging.jmsmonitor;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.logging.JMSLogMessage;
import org.csstudio.platform.utility.jms.JMSConnectionFactory;

/** Data model for the JMS Monitor
 *  @author Kay Kasemir
 */
public class Model implements ExceptionListener, MessageListener
{
    final private Connection connection;
    final private Session session;
    final private MessageConsumer consumer;
    
    final private ArrayList<ReceivedMessage> messages =
        new ArrayList<ReceivedMessage>();
    
    private CopyOnWriteArrayList<ModelListener> listeners =
        new CopyOnWriteArrayList<ModelListener>();

    /** Initialize
     *  @param url JMS server URL
     *  @param topic_name JMS topic
     *  @throws Exception on error
     */
    public Model(final String url, final String topic_name) throws Exception
    {
        if (url == null  ||  url.length() <= 0)
            throw new Exception(Messages.ErrorNoURL);
        if (topic_name.length() <= 0)
            throw new Exception(Messages.ErrorNoTopic);
        connection = JMSConnectionFactory.connect(url);
        connection.setExceptionListener(this);
        connection.start();
        session = connection.createSession(/* transacted */ false,
                                           Session.AUTO_ACKNOWLEDGE);
        final Topic topic = session.createTopic(topic_name);
        consumer = session.createConsumer(topic);
        consumer.setMessageListener(this);
        
    }
    
    /** Add listener
     *  @param listener Listener to add
     */
    public void addListener(final ModelListener listener)
    {
        listeners.add(listener);
    }

    /** Remove listener
     *  @param listener Listener to remove
     */
    public void removeListener(final ModelListener listener)
    {
        listeners.remove(listener);
    }
    
    /** @return Array of received messages */
    public ReceivedMessage[] getMessages()
    {
        synchronized (messages)
        {
            final ReceivedMessage retval[] =
                new ReceivedMessage[messages.size()]; 
            return messages.toArray(retval);
        }
    }

    /** Remove all messages */
    public void clear()
    {
        messages.clear();
        fireModelChanged();
    }

    /** Must be called to release resources when no longer used */
    public void close()
    {
        try
        {
            consumer.close();
            session.close();
            connection.close();
        }
        catch (Exception ex)
        {
            CentralLogger.getInstance().getLogger(this).warn(
                    "JMS shutdown error " + ex.getMessage(), ex); //$NON-NLS-1$
        }
    }

    /** @see MessageListener */
    public void onMessage(final Message message)
    {
        if (message instanceof MapMessage)
            handleMapMessage((MapMessage) message);
        else
            CentralLogger.getInstance().getLogger(this).error(
                "Message type " + message.getClass().getName() + " not handled"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /** Handle received MapMessage
     *  @param message The MapMessage
     */
    @SuppressWarnings("unchecked")
    private void handleMapMessage(final MapMessage message)
    {
        try
        {
            final Enumeration<String> names = message.getMapNames();
            final ArrayList<MessageProperty> content =
                new ArrayList<MessageProperty>();
            String type = Messages.UnknownType;
            while (names.hasMoreElements())
            {
                final String name = names.nextElement();
                final String value = message.getString(name);
                if (JMSLogMessage.TYPE.equals(name))
                    type = value;
                else
                    content.add(new MessageProperty(name, value));
            }
            final ReceivedMessage entry = new ReceivedMessage(type, content);
            // Add to end of list
            synchronized (messages)
            {
                messages.add(entry);
            }
            fireModelChanged();
        }
        catch (Exception ex)
        {
            CentralLogger.getInstance().getLogger(this).error(
                    "Message error " + ex.getMessage(), ex); //$NON-NLS-1$
        }
    }

    /** Notify listeners */
    private void fireModelChanged()
    {
        for (ModelListener listener : listeners)
        {
            try
            {
                listener.modelChanged(this);
            }
            catch (Throwable ex)
            {
                CentralLogger.getInstance().getLogger(this).error(ex);
            }
        }
    }

    /** @see ExceptionListener */
    public void onException(final JMSException ex)
    {
        CentralLogger.getInstance().getLogger(this).error(
                "JMS Exception " + ex.getMessage(), ex); //$NON-NLS-1$
    }
}
