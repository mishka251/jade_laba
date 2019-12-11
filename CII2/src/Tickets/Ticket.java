package Tickets;

import Questions.Question;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.*;

//          0      1                   2                  3                   4                      5
enum State {
    Init, AskingQuestions, GettingQuestions, SendComplexityToAll, GettingNeededComplexity, CheckSelfComplexity,
    //6                      7                   8                  9
    SendChangePropose, WaitChangePropose, WaitChangeResponse, ChangeQuestion
};

public class Ticket extends Agent {

    final int delta = 1; // допустимая погрешность сложности билета
    protected Question[] questions;

    private AID myManager;
    private int neededComp; // требуемая сложность

    @Override
    public void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ticket");
        sd.setName("OneTicket");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        questions = new Question[2]; // выделяем память под вопросы
        myManager = (AID) getArguments()[0];
        addBehaviour(new RequestPerformer());
    }

    public void deregister() {
        System.out.println("Agent done");
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    /**
     * Сложность билета
     *
     * @return сложность билета
     */
    public int getTotalComplex() {
        return questions[0].getComplex() + questions[1].getComplex();
    }

    /**
     * Индекс вопроса, который надо отдать
     *
     * @return 0 или 1
     */
    public int getSendIndex() {
        return (getTotalComplex() > neededComp) == (questions[0].getComplex() > questions[1].getComplex()) ? 0 : 1;
    }

    /**
     * Проверяем готовность
     *
     * @return готов/не готов
     */
    public boolean isReady() {
        return (Math.abs(getTotalComplex() - neededComp) <= delta);
    }

    /**
     * На какой вопрос будем менять полученный
     *
     * @param sw вопрос, который будем принимать
     * @return -1 если не меняем, иначе индекс отдаваемого вопроса
     */
    public int needSwap(Question sw) {
        int delta_now = Math.abs(getTotalComplex() - neededComp);
        int delta_change_0 = Math.abs(sw.getComplex() + questions[1].getComplex() - neededComp);
        int delta_change_1 = Math.abs(sw.getComplex() + questions[0].getComplex() - neededComp);

        boolean can_swap_0 = (delta_change_0 < delta_now) && !sw.getNameSubj().equals(questions[1].getNameSubj());
        boolean can_swap_1 = (delta_change_1 < delta_now) && !sw.getNameSubj().equals(questions[0].getNameSubj());

        if (!can_swap_0 && !can_swap_1) {
            return -1;
        }
        if (can_swap_0 && can_swap_1) {
            return delta_change_0 < delta_change_1 ? 0 : 1;
        }
        return can_swap_0 ? 0 : 1;
    }

    /**
     * Нужно ли поменять вопросы
     *
     * @param without индекс вопрсоа, который отдаем
     * @param sw      вопрос, который получаем
     * @return Да/нет нужно ли менять
     */
    public boolean needSwapWithout(int without, Question sw) //Сложность
    {
        int left_ind = 1 - without;//1->0, 0->1

        Question leftQuestion = questions[left_ind];
        if (leftQuestion.getNameSubj().equals(sw.getNameSubj())) {
            return false;
        }
        int newComplexity = leftQuestion.getComplex() + sw.getComplex();
        return Math.abs(getTotalComplex() - neededComp) > Math.abs(newComplexity - neededComp);
    }

    @Override
    public String toString() {
        String newLine = System.lineSeparator();
        return getName() + " :" + " Сложность в билете = " + (questions[0].getComplex() + questions[1].getComplex())
                + newLine + questions[0].toString()
                + newLine + questions[1].toString();
    }


    private class RequestPerformer extends Behaviour {
        ArrayList<DFAgentDescription> questionsAgent, tickets;
        private State step = State.Init;
        private int receiver = 0,
                questionsCount = 0,
                questionForSwapIndex,
                skipCount = 0;
        private boolean done = false;
        private MessageTemplate mt;
        Question proposeQuestion;

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


        void askQuestions() {
            if (questionsCount == 2) {

                System.out.println("\nstart\n" + myAgent.toString());
                System.out.println("--------------------------------------------------------------------");
                if (isReady()) {
                    deregister();
                    done = true;
                }
                step = State.SendComplexityToAll;
                return;
            }
            if (questionsAgent.size() == receiver) {
                deregister();
                done = true;
                return;
            }
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.addReceiver(questionsAgent.get(receiver++).getName());
            if (questionsCount == 1) {
                cfp.setContent("get question;" + questions[0].getNameSubj());
            } else {
                cfp.setContent("get question;_");
            }
            cfp.setReplyWith("cfp" + System.currentTimeMillis());
            cfp.setConversationId("tickets-creation");
            myAgent.send(cfp);
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("tickets-creation"),
                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
            step = State.GettingQuestions;
        }


        @Override
        public void action() {
            switch (step) {
                case Init:
                    questionsAgent = searchByType("question");
                    if (questionsAgent.size() != 0) {
                        step = State.AskingQuestions;
                    } else {
                        System.err.println("Не найдено вопросов");
                        done = true;
                        deregister();
                        return;
                    }
                    break;

                case AskingQuestions:
                    askQuestions();
                    break;

                case GettingQuestions:
                    ACLMessage msg = myAgent.blockingReceive(mt);

                    if (msg.getPerformative() == ACLMessage.PROPOSE) {
                        try {
                            questions[questionsCount++] = (Question) msg.getContentObject();
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                    }
                    step = State.AskingQuestions;
                    break;

                case SendComplexityToAll:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(myManager);
                    cfp.setContent(String.valueOf(getTotalComplex()));
                    myAgent.send(cfp);
                    step = State.GettingNeededComplexity;
                    break;

                case GettingNeededComplexity:
                    mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    msg = myAgent.blockingReceive(mt);
                    neededComp = Integer.parseInt(msg.getContent());

                    step = State.CheckSelfComplexity;
                    receiver = 0;
                    break;

                case CheckSelfComplexity:
                    if (isReady()) {
                        deregister();
                        done = true;
                        return;
                    }
                    skipCount = 0;
                    if (getTotalComplex() > neededComp) {
                        step = State.SendChangePropose;
                    } else {
                        step = State.WaitChangePropose;
                    }

                    break;

                case SendChangePropose:
                    tickets = searchByType("ticket");

                    if (receiver >= tickets.size()) {
                        deregister();
                        done = true;
                        return;
                    }
                    ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                    req.addReceiver(tickets.get(receiver).getName());
                    questionForSwapIndex = getSendIndex();
                    try {
                        req.setContentObject(questions[questionForSwapIndex]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    myAgent.send(req);
                    step = State.WaitChangeResponse;
                    break;

                case WaitChangePropose:
                    tickets = searchByType("ticket");
                    if (tickets.size() == 0) {
                        deregister();
                        done = true;
                        return;
                    }

                    mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                    msg = myAgent.blockingReceive(mt, 1000);
                    if (msg != null) {
                        Question q = null;
                        try {
                            q = (Question) msg.getContentObject();
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                        ACLMessage reply = new ACLMessage(ACLMessage.PROPOSE);
                        assert q != null;
                        questionForSwapIndex = needSwap(q);
                        proposeQuestion = q;
                        if (questionForSwapIndex > -1) {
                            reply.setContent("confirm");
                            try {
                                reply.setContentObject(questions[questionForSwapIndex]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            step = State.ChangeQuestion;
                        } else {
                            reply.setContent("disconfirm");
                        }
                        reply.addReceiver(msg.getSender());
                        myAgent.send(reply);

                    } else {
                        skipCount++;
                        if (skipCount == 100) {
                            deregister();
                            done = true;
                            return;
                        }
                    }
                    break;

                case WaitChangeResponse:
                    mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                    msg = myAgent.blockingReceive(mt, 500);
                    if (msg == null) {
                        step = State.SendChangePropose;
                        skipCount++;
                        if (skipCount == 10) {
                            skipCount = 0;
                            receiver++;
                        }
                        return;
                    }

                    if (!msg.getContent().equalsIgnoreCase("disconfirm")) {
                        Question q = null;
                        try {
                            q = (Question) msg.getContentObject();

                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                        ACLMessage reply = new ACLMessage(ACLMessage.PROPOSE);
                        assert q != null;
                        if (needSwapWithout(questionForSwapIndex, q)) {
                            System.out.println("\r\n" + "Обмен " + myAgent.getLocalName() + " " + questions[questionForSwapIndex] + " - " + q + "  " + msg.getSender().getLocalName() + "\r\n");
                            questions[questionForSwapIndex] = q;
                            reply.setContent("swap");
                        } else {
                            reply.setContent("not swap");
                        }
                        reply.addReceiver(msg.getSender());
                        myAgent.send(reply);
                    }

                    receiver++;
                    step = State.CheckSelfComplexity;
                    break;

                case ChangeQuestion:
                    mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                    msg = myAgent.blockingReceive(mt);

                    if (msg.getContent().equalsIgnoreCase("swap")) {
                        questions[questionForSwapIndex] = proposeQuestion;
                        step = State.CheckSelfComplexity;
                    } else {
                        step = State.WaitChangePropose;
                    }
                    break;
            }
        }

        String newLine = System.lineSeparator();

        @Override
        public boolean done() {

            if (done && Ticket.this.questions[0] != null && Ticket.this.questions[1] != null) // ! Добавить "разные темы"!
            {
                System.out.println(myAgent.toString());

                try (FileWriter writer = new FileWriter("output.txt", true)) {
                    String text = myAgent.toString();
                    writer.write(text + newLine);
                    writer.append(newLine);
                    writer.flush();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
                System.out.println("Агент записал в файл и ушел");
                tickets = searchByType("ticket");
                System.out.println("Билетов осталоссь  " + tickets.size());
            } else if (done) {
                System.out.println("Агенту " + myAgent.getLocalName() + " не осталось вопросов");
            }
            return done;
        }
    }
}