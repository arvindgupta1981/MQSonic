/*
Copyright 2001-2013 Aurea, Inc. All Rights Reserved.

Sample Application

Writing a Basic JMS Application that uses:
    - Publish and Subscribe
    - Durable Subsciptions
    - Persistent Messages

This sample publishes and subscribes to a specified topic.
Text you enter is published to the topic with the user name.
The message will persist for ten minutes if the subscriber is not available.
If the subscriber reconnects within that timem, the message is delivered.

Usage:
  java DurableChat -b <broker:port> -u <username> -p <password>
      -b broker:port points to your message broker
                     Default: localhost:2506
      -u username    must be unique (but is not checked)
      -p password    password for user (not checked)

Suggested demonstration:
  - In separate console windows, start instances of the application
    under unique user names.For example:
       java DurableChat -b localhost:2506 -u ACCOUNTING
       java DurableChat -b localhost:2506 -u LEGAL
  - Enter text and then press Enter to publish the message.
  - See messages appear under the various user names as you
    enter messages in each console window.
  - Stop a session by pressing CTRL+C in its console window.
  - Keep sending messages in other sessions.
  - Restart the subscriber username session that was stopped.
  - Note that the "missed" messages are still available if the restart is
    within thirty minutes.

*/


public class DurableChat implements
     javax.jms.MessageListener      // to handle message subscriptions
{
    private static final String APP_TOPIC = "jms.samples.durablechat";
    private static final String PROPERTY_NAME = "Department";
    private static final String DEFAULT_BROKER_NAME = "localhost:2506";
    private static final String DEFAULT_PASSWORD = "password";
    private static final String CLIENT_ID = "DurableChatter";
    private static final long   MESSAGE_LIFESPAN = 1800000; //30 minutes

    private javax.jms.Connection connection = null;
    private javax.jms.Session pubSession = null;
    private javax.jms.Session subSession = null;


    public void DurableChatter(String broker, String username, String password)
    {
        javax.jms.MessageProducer publisher = null;
        javax.jms.MessageConsumer subscriber = null;
        javax.jms.Topic topic = null;

        //Create a connection:
        try{
            javax.jms.ConnectionFactory factory;
            factory = (new progress.message.jclient.ConnectionFactory (broker));
            connection = factory.createConnection (username, password);

            //Durable Subscriptions are indexed by username, clientID and subscription name
            //It is a good proactice to set the clientID:
            connection.setClientID(CLIENT_ID);
            pubSession = connection.createSession(false,javax.jms.Session.AUTO_ACKNOWLEDGE);
            subSession = connection.createSession(false,javax.jms.Session.AUTO_ACKNOWLEDGE);
        }
        catch (javax.jms.JMSException jmse){
            System.err.println ("Error: Cannot connect to Broker - " + broker);
            jmse.printStackTrace();
            System.exit(1);
        }

        //Create Publisher and Durable Subscriber:
        try{

            topic = pubSession.createTopic(APP_TOPIC);
            subscriber = subSession.createDurableSubscriber(topic, "SampleSubscription");
            subscriber.setMessageListener(this);
            publisher = pubSession.createProducer(topic);
            connection.start();
        }
        catch (javax.jms.JMSException jmse){
            System.out.println("Error: connection not started.");
            jmse.printStackTrace();
            System.exit(1);
        }

        //Wait for user input

        try
        {
            System.out.println("Enter text to send as message and press enter.");
            java.io.BufferedReader stdin =
                new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            while (true)
            {
                String s = stdin.readLine();

                if(s == null){
                    exit();
                }
                else if (s.length()>0)
                {
                    try
                    {
                        javax.jms.TextMessage msg = pubSession.createTextMessage();
                        msg.setText(username + ": " + s);
                        //Publish the message persistantly:
                        publisher.send(
                            msg,                               //message
                            javax.jms.DeliveryMode.PERSISTENT, //publish persistantly
                            javax.jms.Message.DEFAULT_PRIORITY,//priority
                            MESSAGE_LIFESPAN);                 //Time to Live
                    }
                    catch (javax.jms.JMSException jmse){
                        System.err.println("Error publishing message:" + jmse.getMessage());
                    }
                }
            }
        }
        catch (java.io.IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    /** Message Handler**/
    public void onMessage( javax.jms.Message aMessage)
    {
        try
        {
            // Cast the message as a text message.
            javax.jms.TextMessage textMessage = (javax.jms.TextMessage) aMessage;

            // This handler reads a single String from the
            // message and prints it to the standard output.
            try
            {
                String string = textMessage.getText();
                System.out.println( string );
            }
            catch (javax.jms.JMSException jmse)
            {
                jmse.printStackTrace();
            }
        }
        catch (java.lang.RuntimeException rte)
        {
            rte.printStackTrace();
        }
    }



    /** Cleanup resources cleanly and exit. */
    private void exit()
    {
        try
        {
            connection.close();
        }
        catch (javax.jms.JMSException jmse)
        {
            jmse.printStackTrace();
        }

        System.exit(0);
    }

    //
    // NOTE: the remainder of this sample deals with reading arguments
    // and does not utilize any JMS classes or code.
    //

    /** Main program entry point. */
    public static void main(String argv[]) {
        // Is there anything to do?
        if (argv.length == 0) {
            printUsage();
            System.exit(1);
            
            
        }

        // Values to be read from parameters
        String broker    = DEFAULT_BROKER_NAME;
        String username  = null;
        String password  = DEFAULT_PASSWORD;

        // Check parameters
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];


            if (arg.equals("-b")) {
                if (i == argv.length - 1 || argv[i+1].startsWith("-")) {
                    System.err.println("error: missing broker name:port");
                    System.exit(1);
                }
                broker = argv[++i];
                continue;
            }

            if (arg.equals("-u")) {
                if (i == argv.length - 1 || argv[i+1].startsWith("-")) {
                    System.err.println("error: missing user name");
                    System.exit(1);
                }
                username = argv[++i];
                continue;
            }

            if (arg.equals("-p")) {
                if (i == argv.length - 1 || argv[i+1].startsWith("-")) {
                    System.err.println("error: missing password");
                    System.exit(1);
                }
                password = argv[++i];
                continue;
            }

            if (arg.equals("-h")) {
                printUsage();
                System.exit(1);
            }

            // Invalid argument
            System.err.println ("error: unexpected argument: "+arg);
            printUsage();
            System.exit(1);
        }

        // Check values read in.
        if (username == null) {
            System.err.println ("error: user name must be supplied");
            printUsage();
        }

        // Start the JMS client for the "chat".
        DurableChat durableChat = new DurableChat();
        durableChat.DurableChatter (broker, username, password);

    }

    /** Prints the usage. */
    private static void printUsage() {

        StringBuffer use = new StringBuffer();
        use.append("usage: java DurableChat (options) ...\n\n");
        use.append("options:\n");
        use.append("  -b name:port Specify name:port of broker.\n");
        use.append("               Default broker: "+DEFAULT_BROKER_NAME+"\n");
        use.append("  -u name      Specify unique user name. (Required)\n");
        use.append("  -p password  Specify password for user.\n");
        use.append("               Default password: "+DEFAULT_PASSWORD+"\n");
        use.append("  -h           This help screen.\n");
        System.err.println (use);
    }

}

