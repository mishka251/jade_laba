package Tickets;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
//import jade.lang.acl.UnreadableException;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class Tickets extends Agent {
    private int totalComplexity;
    int ticketsCount;
    CyclicBehaviour managerBehaviour = null;

    @Override
    protected void setup() {

        //--- Clear
        try {
            FileOutputStream writer = new FileOutputStream("output.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
//        //---


        TicketGui myGui = new TicketGui(this);
        myGui.showGui();
    }

    public void getTickets(int ticketsCount) {
        this.ticketsCount = ticketsCount;
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                ContainerController cc = getContainerController();
                for (int i = 0; i < ticketsCount; i++) {
                    try {
                        cc.createNewAgent("ticketAgent " + System.currentTimeMillis(), "Tickets.Ticket", new Object[]{myAgent.getAID()}).start();
                    } catch (StaleProxyException e) {
                        e.printStackTrace();
                    }
                    doWait(1);
                }
                if (managerBehaviour == null) {
                    managerBehaviour = new TicketsManager();
                    myAgent.addBehaviour(managerBehaviour);
                }
            }
        });
    }


    private class TicketsManager extends CyclicBehaviour {
        private int receiveCount = 0;
        ArrayList<DFAgentDescription> tickets;

        ArrayList<DFAgentDescription> searchByType(String type) {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(type);
            template.addServices(sd);
            ArrayList<DFAgentDescription> desc = new ArrayList<>();
            try {
                desc.addAll(Arrays.asList(DFService.search(myAgent, template)));
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
            for (DFAgentDescription a : desc) {
                if (a.getName().equals(myAgent.getAID())) {
                    desc.remove(a);
                    break;
                }
            }
            return desc;
        }


        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.blockingReceive(mt, 500);
            if (msg != null) {
                System.out.println("Tickets get some");
                int c = Integer.parseInt(msg.getContent());
                totalComplexity += c;
                receiveCount++;
                tickets = searchByType("ticket");
                if (receiveCount == tickets.size()) {
                    int middleCompl = totalComplexity / tickets.size();
                    System.out.println("=======================================================================" +
                            "==================================");
                    System.out.println("Общая сложность: " + totalComplexity);
                    System.out.println("Средняя сложность: " + middleCompl);
                    System.out.println("=======================================================================" +
                            "==================================");

                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (DFAgentDescription a : tickets) {
                        cfp.addReceiver(a.getName());
                    }
                    cfp.setContent(String.valueOf(middleCompl));
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);
                    totalComplexity = 0;
                    receiveCount = 0;
                }
            }
        }
    }
}