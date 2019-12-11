package Questions;

import java.io.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jade.core.Agent;
//import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
//import jade.lang.acl.ACLMessage;
//import jade.lang.acl.MessageTemplate;
//import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.ListIterator;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;

public class Questions extends Agent {
    //private ArrayList<Question> sendQ = new ArrayList<>(); //вопросы, кот мы послали в билет
//    private int                 minComp = 0, maxComp = 0;
//    private int lastQFound = 0;
//    private int questionCount = 0;


    @Override
    public void setup() {
        //все вопросы
        ArrayList<Question> data = new ArrayList<>();

        this.addQuestion();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("questions-agent");
        sd.setName("Questions-storage");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        //addBehaviour(new RequestsServer());
    }

    public void addQuestion() {

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {

                // УКАЗАТЬ ЧИСЛО ВОПРОСОВ
//                int N = 24; //12
//                String[] namesSubj = new String[N];
//                int[] compl = new int[N];
//                String[] questions = new String[N];

                String fileName = "input.txt";
               // String content;
                try {
                    readFile(fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                try {
//                    content = Files.lines(Paths.get(fileName)).reduce("", String::concat);
//
//                    //System.out.println(content);
//
//                    String[] strs = new String[N];
//                    int i = 0;
//
//                    // Разбиение по строкам, запись строк в strs
//                    for (String retval : content.split("]", N)) {
//                        strs[i] = retval;
//                        //System.out.println(retval);
//                        i += 1;
//                    }
////                    for (int z = 0; z < strs.length; z++) {
////                        //System.out.println(strs[z]);
////                    }
//
//                    // Разбиение по свойствам
//                    System.out.println("\n\n Вопросы к билетам");
//                    String[][] storage = new String[strs.length][3];
//
//                    for (int count = 0; count < strs.length; count++) {
//                        i = 0;
//                        //String сont = strs[count];
//
//                        //String сont = new String("fdf");
//                        String cont = strs[count];
//                        for (String retval : cont.split(";", 3)) {
//                            storage[count][i] = retval;
//                            //System.out.println(retval);
//                            //System.out.println(storage[count][i]);
//                            i += 1;
//                        }
//                    }
//                    for (int count = 0; count < N; count++) {
//                        namesSubj[count] = storage[count][0];
//                        questions[count] = storage[count][1];
//                        compl[count] = Integer.parseInt(storage[count][2]);
//                    }
//                } catch (IOException ex) {
//                }
                //КОНЕЦ ВСТАВКИ
                // создание билетов из файлов
                //Random rnd = new Random();
//                ContainerController cc = getContainerController();
//                for (int count = 1; count < N; count++)
//                    try {
//                        cc.createNewAgent("questionAgent" + System.currentTimeMillis(), "Questions.Question",
//                                new Object[]{questions[count], namesSubj[count], compl[count], 1}).start();
///*                    cc.createNewAgent("questionAgent" + System.currentTimeMillis(), "Questions.Question",
//                                                           new Object[]{nameQuestion, nameSubj,complex, 2}).start();*/
//                        doWait(5);
//                    } catch (StaleProxyException e) {
//                        e.printStackTrace();
//                    }
            }
        });

    }

    class QuestionInfo {
        /**
         * текст вопроса
         */
        public String nameQuestion;
        /**
         * название предмета
         */
        public String nameSubj;
        /**
         * сложность
         */
        public int complex;
    }

    QuestionInfo parseStr(String line) {
        QuestionInfo obj = new QuestionInfo();
        String[] vals = line.split(";");
        obj.nameSubj = vals[0];
        obj.nameQuestion = vals[1];
        obj.complex = Integer.parseInt(vals[2]);
        return obj;
    }

    void readFile(String fileName) throws IOException {
        ContainerController cc = getContainerController();
        Files.lines(Paths.get(fileName))
                .map(this::parseStr)
                .map(qi -> new Object[]{qi.nameQuestion, qi.nameSubj, qi.complex, 1})
                .forEach(obj -> {
                    try {
                        cc.createNewAgent("questionAgent" + System.currentTimeMillis(),
                                "Questions.Question",
                                obj).start();
                    } catch (StaleProxyException e) {
                        e.printStackTrace();
                    }
                });
        ;
    }

//    public Question getQuestion(String nameSubj) {
//        for (Question q : data) {
//            if (q.getNameSubj().equalsIgnoreCase(nameSubj)) {
//                return q;
//            }
//        }
//        return null;
//    }

//    public Question getQuestion(int complex, String subj) {
//        ListIterator<Question> iter;
//        for (iter = data.listIterator(); iter.hasNext(); ) {
//            int t = iter.nextIndex();
//            Question q = iter.next();
//            if (q.getComplex() == complex && !q.getNameSubj().equals(subj) && !sendQ.contains(q)) {
//                return q;
//            }
//        }
//        return null;
//    }

//    private class RequestsServer extends CyclicBehaviour {
//        private int sendQuestionRand = 0; // Real random number
//
//        @Override
//        public void action() {
//
//        }
//    }

}
