package edu.lwtech.csd299.topten;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import org.apache.log4j.*;
import freemarker.template.*;

@WebServlet(name = "TopTenListsServlet", urlPatterns = {"/lists"}, loadOnStartup = 0)
public class TopTenListsServlet extends HttpServlet {

    private static final long serialVersionUID = 2020111122223333L;
    private static final Logger logger = Logger.getLogger(TopTenListsServlet.class);

    private static final String TEMPLATE_DIR = "/WEB-INF/classes/templates";
    private static final Configuration freemarker = new Configuration(Configuration.getVersion());

    private DAO<Member> membersDAO = null;
    private DAO<TopTenList> listsDAO = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        logger.warn("=========================================");
        logger.warn("  TopTenListsServlet init() started");
        logger.warn("    http://localhost:8080/topten/lists");
        logger.warn("=========================================");

        logger.info("Getting real path for templateDir");
        String templateDir = config.getServletContext().getRealPath(TEMPLATE_DIR);
        logger.info("...real path is: " + templateDir);
        
        logger.info("Initializing Freemarker. templateDir = " + templateDir);
        try {
            freemarker.setDirectoryForTemplateLoading(new File(templateDir));
        } catch (IOException e) {
            logger.error("Template directory not found in directory: " + templateDir, e);
        }
        logger.info("Successfully Loaded Freemarker");

        // String jdbc = "jdbc:mariadb://localhost:3306/topten?useSSL=false&allowPublicKeyRetrieval=true";
        String jdbc = "jdbc:mariadb://csd299.cv18zcsjzteu.us-west-2.rds.amazonaws.com:3306/topten?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "topten";
        String password = "lwtech2000";
        String driver = "org.mariadb.jdbc.Driver";      // The MariaDB driver works better than the MySQL driver

        // ======== UNCOMMENT TO USE MEMORY DAOs ========
        // membersDao = new MemberMemoryDAO();
        // listsDao = new TopTenListMemoryDAO();

        // ======== UNCOMMENT TO USE SQL DAOs ========
        membersDAO = new MemberSqlDAO();
        listsDAO = new TopTenListSqlDAO();

        membersDAO.init(jdbc, user, password, driver);
        listsDAO.init(jdbc, user, password, driver);

        logger.warn("Initialize complete!");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("IN - GET " + request.getRequestURI());
        long startTime = System.currentTimeMillis();

        String command = request.getParameter("cmd");
        if (command == null) command = "home";

        int owner = 0;
        boolean loggedIn = false;
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                owner = (Integer)session.getAttribute("owner");
                loggedIn = true;
            } catch (NumberFormatException e) {
                owner = 0;
                loggedIn = false;
            }
        }

        String template = null;                 // 404 will be returned if template isn't set inside the switch statement
        TopTenList list = null;
        Map<String, Object> model = new HashMap<>();
        model.put("owner", owner);
        model.put("loggedIn", loggedIn);

        switch (command) {

            case "add":
                template = "add.ftl";
                break;

            case "home":
                List<TopTenList> topTenLists = listsDAO.getAll();
                model.put("topTenLists", topTenLists);
                template = "home.ftl";
                break;

            case "login":
                template = "login.ftl";
                break;

            case "logout":
                if (session != null) {
                    session.invalidate();
                }
                template = "confirm.ftl";
                model.put("message", "You have been successfully logged out.<br /><a href='?cmd=home'>Home</a>");
                break;

            case "register":
                if (session != null) {
                    session.invalidate();
                }
                template = "register.ftl";
                break;

            case "like":
                int id = parseInt(request.getParameter("id"));
                if (id < 0) break;

                list = listsDAO.getByID(id);
                if (list == null) break;

                list.addLike();
                // FALL THRU TO case "show" !!!

            case "show":
                int index = parseInt(request.getParameter("index"));
                if (index < 0) index = 0;

                int numItems = listsDAO.getAllIDs().size();
                int nextIndex = (index + 1) % numItems;
                int prevIndex = index - 1;
                if (prevIndex < 0) prevIndex = numItems-1;

                template = "show.ftl";
                if (list == null) {                                 // i.e., if we didn't fall thru from the "like" case
                    list = listsDAO.getByIndex(index);
                }
                list.addView();
                listsDAO.update(list);
                model.put("topTenList", list);
                model.put("listNumber", index+1);                   // Java uses 0-based indexes.  Users want to see 1-based indexes.
                model.put("prevIndex", prevIndex);
                model.put("nextIndex", nextIndex);
                break;
                
            default:
                logger.debug("Unknown GET command received: " + command);
                break;
        }

        if (template == null) {
            // Send 404 error response
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e)  {
                logger.error("IO Error: ", e);
            }
            return;
        }

        processTemplate(response, template, model);

        long time = System.currentTimeMillis() - startTime;
        logger.info("OUT- GET " + request.getRequestURI() + " " + time + "ms");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("IN -POST " + request.getRequestURI());
        long startTime = System.currentTimeMillis();
        
        String command = request.getParameter("cmd");
        if (command == null) command = "";

        int owner = 0;
        boolean loggedIn = false;
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                owner = Integer.parseInt((String)session.getAttribute("owner"));
                loggedIn = true;
            } catch (NumberFormatException e) {
                owner = 0;
                loggedIn = false;
            }
        }

        String message = "";
        String template = "confirm.ftl";
        Map<String, Object> model = new HashMap<>();
        String username = "";
        String password = "";
        Member member;
        List<Member> members;
        
        switch (command) {

            case "create":
                TopTenList newList = getTopTenListFromRequest(request, owner);

                if (newList == null) {
                    logger.info("Create request ignored because one or more fields were empty.");
                    message = "Your new TopTenList was not created because one or more fields were empty.<br /><a href='?cmd=home'>Home</a>";
                    break;
                }

                if (listsDAO.insert(newList) > 0)
                    message = "Your new TopTen List has been created successfully.<br /><a href='?cmd=home'>Home</a>";
                else
                    message = "There was a problem adding your list to the database.<br /><a href='?cmd=home'>Home</a>";
                break;

            case "login":
                username = request.getParameter("username");
                password = request.getParameter("password");
                
                members = membersDAO.search(username);
                if (members == null || members.isEmpty()) {
                    message = "We do not have a member with that username on file. Please try again.<br /><a href='?cmd=login'>Log In</a>";
                    model.put("loggedIn", loggedIn);
                    model.put("message", message);
                    break;
                }

                member = members.get(0);
                if (member.getPassword().equals(password)) {
                    owner = member.getID();
                    loggedIn = true;

                    session = request.getSession(true);
                    session.setAttribute("owner", owner);

                    message = "You have been successfully logged in to your account.<br /><a href='?cmd=show'>Show Lists</a>";
                } else {
                    message = "Your password did not match what we have on file.  Please try again.<br /><a href='?cmd=login'>Log In</a>";
                }

                model.put("loggedIn", loggedIn);
                model.put("message", message);
                break;

            case "register":
                username = request.getParameter("username");
                password = request.getParameter("password");
                
                members = membersDAO.search(username);
                if (members != null && !members.isEmpty()) {
                    message = "That username is already registered here. Please use a different username.<br /><a href='?cmd=login'>Log In</a>";
                    model.put("message", message);
                    break;
                }

                member = new Member(username, password);
                membersDAO.insert(member);

                message = "Welcome to TopTopTenLists.com!  You are now a registered member. Please <a href='?cmd=login'>log in</a>.";
                model.put("message", message);
                break;

            default:
                logger.info("Unknown POST command received: " + command);

                // Send 404 error response
                try {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                } catch (IOException e)  {
                    logger.error("IO Error: ", e);
                }
                return;
        }

        model.put("message", message);
       
        processTemplate(response, template, model);

        long time = System.currentTimeMillis() - startTime;
        logger.info("OUT- GET " + request.getRequestURI() + " " + time + "ms");
    }
    
    @Override
    public void destroy() {
        logger.warn("-----------------------------------------");
        logger.warn("  TopTenListsServlet destroy() completed");
        logger.warn("-----------------------------------------");
        listsDAO.disconnect();
        membersDAO.disconnect();
    }

    @Override
    public String getServletInfo() {
        return "topten-lists-servlet Servlet";
    }

    // ========================================================================

    private int parseInt(String s) {
        int i = -1;
        if (s != null) {
            try {
                i = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                i = -2;
            }
        }
        return i;
    }

    private void processTemplate(HttpServletResponse response, String template, Map<String, Object> model) {
        logger.debug("Processing Template: " + template);
        
        try (PrintWriter out = response.getWriter()) {
            Template view = freemarker.getTemplate(template);
            view.process(model, out);
        } catch (TemplateException | MalformedTemplateNameException e) {
            logger.error("Template Error: ", e);
        } catch (IOException e) {
            logger.error("IO Error: ", e);
        } 
    }

    private TopTenList getTopTenListFromRequest(HttpServletRequest request, int owner) {

        String description = request.getParameter("description");
        if (description == null) return null;

        List<String> items = new ArrayList<>();
        for (int i=10; i >= 1; i--) {
            String item = request.getParameter("item" + i);
            if (item == null || item.isEmpty())
                return null;
            items.add(item);
        }
        
        return new TopTenList(description, items, owner);
    }

}
