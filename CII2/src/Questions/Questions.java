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


public class Questions extends Agent {


    @Override
    public void setup() {

        String fileName = "input.txt";

        try {
            readFile(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
    }

    /**
     * Парсинг строки входного файла в массив объектов инициализации вопроса
     *
     * @param line строка входного файла формат nameSub;nameQuestion;comples
     * @return массив обхектов для инициализации вопрсоа
     */
    Object[] parseStr(String line) {
        String[] vals = line.split(";");
        String nameSubj = vals[0];
        String nameQuestion = vals[1];
        int complex = Integer.parseInt(vals[2]);
        return new Object[]{nameQuestion, nameSubj, complex, 1};
    }

    /**
     * Инициализация агента-вопрсоа
     *
     * @param cc   контроллер контейнера JADE
     * @param args аргументы агента
     */
    void initQuestion(ContainerController cc, Object[] args) {
        try {
            cc.createNewAgent("questionAgent" + System.currentTimeMillis()+args[0],
                    "Questions.Question",
                    args).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    /**
     * Чтение файла и создание агентов
     *
     * @param fileName имя файла с вопросами
     * @throws IOException если нет файла
     */
    void readFile(String fileName) throws IOException {
        ContainerController cc = getContainerController();
        Files.lines(Paths.get(fileName))
                .map(this::parseStr)
                .forEach(obj -> initQuestion(cc, obj));
    }
}
