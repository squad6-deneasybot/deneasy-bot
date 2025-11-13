package com.squad6.deneasybot.service;

import com.squad6.deneasybot.exception.*;
import com.squad6.deneasybot.model.*;
import com.squad6.deneasybot.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.squad6.deneasybot.repository.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class WebhookOrchestratorService {
    private static final Logger logger = LoggerFactory.getLogger(WebhookOrchestratorService.class);

    private final AuthService authService;
    private final CompanyService companyService;
    private final UserService userService;
    private final MenuService menuService;
    private final ReportService reportService;
    private final FaqService faqService;

    private final ChatStateService chatStateService;
    private final JwtUtil jwtUtil;
    private final WhatsAppService whatsAppService;
    private final WhatsAppFormatterService formatterService;
    private final UserRepository userRepository;

    public WebhookOrchestratorService(AuthService authService, CompanyService companyService,
                                      UserService userService,
                                      MenuService menuService, ReportService reportService, FaqService faqService, ChatStateService chatStateService,
                                      JwtUtil jwtUtil, WhatsAppService whatsAppService,
                                      WhatsAppFormatterService formatterService, UserRepository userRepository) {
        this.authService = authService;
        this.companyService = companyService;
        this.userService = userService;
        this.menuService = menuService;
        this.reportService = reportService;
        this.faqService = faqService;
        this.chatStateService = chatStateService;
        this.jwtUtil = jwtUtil;
        this.whatsAppService = whatsAppService;
        this.formatterService = formatterService;
        this.userRepository = userRepository;
    }

    @Async
    public void processMessage(String userPhone, String messageText) {

        if ("menu".equalsIgnoreCase(messageText.trim())) {
            try {
                UserProfile profile = getUserProfile(userPhone);

                logger.info("Usuário {} solicitou o menu principal (Comando Global).", userPhone);
                chatStateService.setState(userPhone, ChatState.AUTHENTICATED);
                whatsAppService.sendMessage(userPhone, formatterService.formatMenu(profile));

                return;

            } catch (ResourceNotFoundException e) {
                logger.warn("Usuário {} (não autenticado) digitou 'menu'. Deixando o fluxo normal tratar.", userPhone);
            }
        }

        synchronized (userPhone.intern()) {

            ChatState currentState = chatStateService.getState(userPhone);

            try {
                switch (currentState) {
                    case START:
                        handleStateStart(userPhone, messageText);
                        break;
                    case AWAITING_APP_KEY:
                        handleStateAwaitingAppKey(userPhone, messageText);
                        break;
                    case AWAITING_APP_SECRET:
                        handleStateAwaitingAppSecret(userPhone, messageText);
                        break;
                    case AWAITING_EMAIL:
                        handleStateAwaitingEmail(userPhone, messageText);
                        break;
                    case AWAITING_EMAIL_CODE:
                        handleStateAwaitingEmailCode(userPhone, messageText);
                        break;
                    case AUTHENTICATED:
                        handleStateAuthenticated(userPhone, messageText);
                        break;
                    case AWAITING_POST_ACTION:
                        handleStateAwaitingPostAction(userPhone, messageText);
                        break;
                    case AWAITING_FAQ_CHOICE: // <-- Da branch FAQ
                        handleStateAwaitingFaqChoice(userPhone, messageText);
                        break;
                    case AWAITING_CRUD_MENU_CHOICE:
                        handleStateCrudMenuChoice(userPhone, messageText);
                        break;
                    case AWAITING_CRUD_ADD_NAME:
                        handleStateCrudAddName(userPhone, messageText);
                        break;
                    case AWAITING_CRUD_ADD_EMAIL:
                        handleStateCrudAddEmail(userPhone, messageText);
                        break;
                    case AWAITING_CRUD_ADD_PHONE:
                        handleStateCrudAddPhone(userPhone, messageText);
                        break;
                    case AWAITING_CRUD_REMOVE_EMAIL_CONFIRM:
                        handleStateCrudRemoveEmailConfirm(userPhone, messageText);
                        break;
                    case AWAITING_CRUD_UPDATE_SELECT_USER:
                        handleStateCrudUpdateSelectUser(userPhone, messageText);
                        break;
                    case AWAITING_CRUD_UPDATE_SELECT_FIELD:
                        handleStateCrudUpdateSelectField(userPhone, messageText);
                        break;
                    case AWAITING_CRUD_UPDATE_NEW_VALUE:
                        handleStateCrudUpdateNewValue(userPhone, messageText);
                        break;
                    case AWAITING_CRUD_POST_ACTION:
                        handleStateCrudPostAction(userPhone, messageText);
                        break;
                }
            } catch (Exception e) {
                logger.error("Erro inesperado ao processar mensagem para {}: {}", userPhone, e.getMessage(), e);

                if (currentState == ChatState.AUTHENTICATED ||
                        currentState == ChatState.AWAITING_POST_ACTION ||
                        currentState == ChatState.AWAITING_CRUD_MENU_CHOICE ||
                        currentState == ChatState.AWAITING_CRUD_POST_ACTION) // Adicionado
                {
                    UserProfile profile = getUserProfile(userPhone);
                    whatsAppService.sendMessage(userPhone, "Ocorreu um erro inesperado. Estamos te retornando ao menu principal.\n\n" + formatterService.formatMenu(profile));
                    chatStateService.setState(userPhone, ChatState.AUTHENTICATED);
                } else {
                    if (currentState.name().startsWith("AWAITING_CRUD_")) {
                        chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_MENU_CHOICE);
                        whatsAppService.sendMessage(userPhone, "😕 Ops, algo deu errado. Voltando ao menu de gerenciamento.\n\n" + formatterService.formatCrudMenu());
                    } else {
                        whatsAppService.sendMessage(userPhone, formatterService.formatFallbackError());
                        chatStateService.setState(userPhone, ChatState.START);
                    }
                }
            }
        }
    }


    private void handleStateStart(String userPhone, String messageText) {
        try {
            ValidatePhoneResponseDTO response = authService.validatePhone(new ValidatePhoneRequestDTO(userPhone));
            UserDTO user = response.user();

            if (user.getSessionToken() != null && jwtUtil.isTokenValid(user.getSessionToken())) {
                chatStateService.setState(userPhone, ChatState.AUTHENTICATED);
                String menu = formatterService.formatMenu(user.getProfile());
                whatsAppService.sendMessage(userPhone, "Olá de volta, " + user.getName() + "!\n\n" + menu);
            } else {
                logger.info("Token inválido para {}. Iniciando fluxo de login...", userPhone);
                SendEmailCodeResponseDTO codeResponse = authService.requestEmailCode(new SendEmailCodeRequestDTO(user));

                chatStateService.saveData(userPhone, "temp_token_hash", codeResponse.hashToken());
                chatStateService.saveData(userPhone, "temp_user_dto", user);
                chatStateService.saveData(userPhone, "context", Context.LOGIN);
                chatStateService.setState(userPhone, ChatState.AWAITING_EMAIL_CODE);

                whatsAppService.sendMessage(userPhone, "Olá, " + user.getName() + ". Para sua segurança, enviamos um código de 6 dígitos para o seu e-mail. Por favor, digite-o:");
            }

        } catch (UserNotFoundByPhoneException e) {
            logger.info("Usuário {} não encontrado. Iniciando fluxo de registro.", userPhone);
            chatStateService.setState(userPhone, ChatState.AWAITING_APP_KEY);
            chatStateService.saveData(userPhone, "context", Context.REGISTRATION);
            whatsAppService.sendMessage(userPhone, "Olá! 👋 Bem-vindo ao DeneasyBot. Para começar, por favor, digite sua *App Key* do ERP:");
        }
    }

    private void handleStateAwaitingAppKey(String userPhone, String messageText) {
        chatStateService.saveData(userPhone, "temp_app_key", messageText.trim());
        chatStateService.setState(userPhone, ChatState.AWAITING_APP_SECRET);
        whatsAppService.sendMessage(userPhone, "App Key recebida. Agora, por favor, digite sua *App Secret*:");
    }

    private void handleStateAwaitingAppSecret(String userPhone, String messageText) {
        String appKey = chatStateService.getData(userPhone, "temp_app_key", String.class)
                .orElseThrow(() -> new java.util.NoSuchElementException("App Key missing for user " + userPhone + " in registration flow"));
        String appSecret = messageText.trim();

        try {
            CompanyDTO companyDTO = authService.validateCompany(appKey, appSecret);

            chatStateService.saveData(userPhone, "temp_company_dto", companyDTO);
            chatStateService.setState(userPhone, ChatState.AWAITING_EMAIL);
            whatsAppService.sendMessage(userPhone, "Empresa *" + companyDTO.getCompanyName() + "* validada com sucesso! ✅\n\nAgora, qual o seu e-mail de gestor cadastrado no ERP?");

        } catch (InvalidKeysInErpException e) {
            logger.warn("Chaves inválidas para {}.", userPhone);
            chatStateService.setState(userPhone, ChatState.AWAITING_APP_KEY);
            whatsAppService.sendMessage(userPhone, "❌ Ops! Essas credenciais (App Key/Secret) parecem inválidas. Vamos tentar de novo.\n\nPor favor, digite sua *App Key*:");
        }
    }

    private void handleStateAwaitingEmail(String userPhone, String messageText) {
        CompanyDTO companyDTO = chatStateService.getData(userPhone, "temp_company_dto", CompanyDTO.class)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "Company data (temp_company_dto) missing for userPhone: " + userPhone));
        String email = messageText.trim();

        try {
            VerifyEmailResponseDTO erpResponse = authService.validateUserInErp(new VerifyEmailRequestDTO(companyDTO.getAppKey(), companyDTO.getAppSecret(), email));
            UserDTO erpUser = erpResponse.user();

            SendEmailCodeResponseDTO codeResponse = authService.requestEmailCode(new SendEmailCodeRequestDTO(erpUser));

            chatStateService.saveData(userPhone, "temp_token_hash", codeResponse.hashToken());
            chatStateService.saveData(userPhone, "temp_user_dto", erpUser);
            chatStateService.setState(userPhone, ChatState.AWAITING_EMAIL_CODE);

            whatsAppService.sendMessage(userPhone, "E-mail encontrado para *" + erpUser.getName() + "*! 👍\n\nEnviamos um código de 6 dígitos para " + erpUser.getEmail() + ". Por favor, digite-o:");

        } catch (UserNotFoundInErpException e) {
            logger.warn("E-mail {} não encontrado para {}.", email, userPhone);
            whatsAppService.sendMessage(userPhone, "❌ E-mail não encontrado nesta empresa. Por favor, verifique e digite o e-mail correto:");
        }
    }

    private void handleStateAwaitingEmailCode(String userPhone, String messageText) {
        String inputCode = messageText.trim();
        String hash = chatStateService.getData(userPhone, "temp_token_hash", String.class)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "Token hash (temp_token_hash) missing for userPhone: " + userPhone));

        UserDTO userDTO = chatStateService.getData(userPhone, "temp_user_dto", UserDTO.class)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "User DTO (temp_user_dto) missing for userPhone: " + userPhone));
        Context context = chatStateService.getData(userPhone, "context", Context.class).orElse(Context.REGISTRATION);

        try {
            VerifyEmailCodeRequestDTO request = new VerifyEmailCodeRequestDTO(hash, context, userDTO);
            VerifyEmailCodeResponseDTO response = authService.verifyEmailCode(hash, inputCode, request);

            UserDTO verifiedUser = response.user();

            if (context == Context.REGISTRATION) {
                CompanyDTO companyDTO = chatStateService.getData(userPhone, "temp_company_dto", CompanyDTO.class)
                        .orElseThrow(() -> new java.util.NoSuchElementException(
                                "Company DTO (temp_company_dto) missing for userPhone: " + userPhone));

                Company savedCompany = companyService.createCompany(companyDTO);

                verifiedUser.setCompanyId(savedCompany.getId());
                verifiedUser.setProfile(UserProfile.MANAGER);
                userService.createUser(verifiedUser);
            }

            chatStateService.clearData(userPhone);
            chatStateService.setState(userPhone, ChatState.AUTHENTICATED);

            String menu = formatterService.formatMenu(verifiedUser.getProfile());
            whatsAppService.sendMessage(userPhone, "Código correto! 🎉 Você está autenticado.\n\n" + menu);

        } catch (InvalidCredentialsException e) {
            logger.warn("Código inválido para {}.", userPhone);
            whatsAppService.sendMessage(userPhone, "❌ Código inválido. Por favor, tente novamente:");
        }
    }

    private void handleStateAuthenticated(String userPhone, String messageText) {

        try {
            String actionResponse = menuService.processMenuOption(userPhone, messageText);
            whatsAppService.sendMessage(userPhone, actionResponse);
            String option = messageText.trim();

            if ("1".equals(option)) {
                transitionToMainMenuPostAction(userPhone);

            } else if ("2".equals(option)) {
                chatStateService.setState(userPhone, ChatState.AWAITING_FAQ_CHOICE);

            } else if ("3".equals(option)) {
                chatStateService.clearAll(userPhone);

            } else if ("4".equals(option) && getUserProfile(userPhone) == UserProfile.MANAGER) {
                chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_MENU_CHOICE);

            } else if ("5".equals(option)) {
                chatStateService.setState(userPhone, ChatState.AWAITING_WISHLIST);
            } else {
                throw new IllegalArgumentException("Opção não tratada no switch de estado do Orchestrator: " + option);
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Opção inválida '{}' para usuário {}", messageText, userPhone);
            UserProfile profile = getUserProfile(userPhone);
            whatsAppService.sendMessage(userPhone, formatterService.formatFallbackError() + "\n\n" + formatterService.formatMenu(profile));
            chatStateService.setState(userPhone, ChatState.AUTHENTICATED);
        }
    }


    private void handleStateAwaitingPostAction(String userPhone, String messageText) {
        UserProfile profile = getUserProfile(userPhone);

        switch (messageText.trim()) {
            case "1":
                chatStateService.setState(userPhone, ChatState.AUTHENTICATED);
                String menu = formatterService.formatMenu(profile);
                whatsAppService.sendMessage(userPhone, menu);
                break;

            case "2":
                chatStateService.clearAll(userPhone);
                String humanContactMessage = "Para prosseguir com o *atendimento humano*, por favor, entre em contato com o número: \n\n" +
                        "*+55 79 99999-9999*\n\n" +
                        "Agradecemos seu contato. Obrigado por usar o DeneasyBot!👋";
                whatsAppService.sendMessage(userPhone, humanContactMessage);
                break;

            case "3":
                chatStateService.clearAll(userPhone);
                whatsAppService.sendMessage(userPhone, "Atendimento encerrado. Obrigado por usar o DeneasyBot! 👋");
                break;

            default:
                whatsAppService.sendMessage(userPhone, formatterService.formatFallbackError() + "\n\n" + formatterService.formatPostActionMenu());
                break;
        }
    }

    private void transitionToMainMenuPostAction(String userPhone) {
        chatStateService.setState(userPhone, ChatState.AWAITING_POST_ACTION);
        whatsAppService.sendMessage(userPhone, formatterService.formatPostActionMenu());
    }

    private void transitionToCrudPostAction(String userPhone) {
        chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_POST_ACTION);
        whatsAppService.sendMessage(userPhone, formatterService.formatCrudPostActionMenu());
    }

    private void transitionToCrudMenu(String userPhone, UserProfile profile) {
        chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_MENU_CHOICE);
        whatsAppService.sendMessage(userPhone, formatterService.formatCrudMenu());
    }

    private User getUserByPhone(String userPhone) {
        return userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado pelo telefone: " + userPhone));
    }

    private void handleStateCrudMenuChoice(String userPhone, String messageText) {
        User manager = getUserByPhone(userPhone);
        String option = messageText.trim().toUpperCase();

        switch (option) {
            case "1" -> {
                List<UserDTO> employees = userService.getAllEmployees(manager);
                whatsAppService.sendMessage(userPhone, formatterService.formatEmployeeList(employees));
                whatsAppService.sendMessage(userPhone, formatterService.formatCrudMenu());
            }
            case "2" -> {
                chatStateService.saveData(userPhone, "crud_add_company_id", manager.getCompany().getId());
                whatsAppService.sendMessage(userPhone, "Ok, vamos adicionar um funcionário. Qual é o nome completo dele?");
                chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_ADD_NAME);
            }
            case "3" -> {
                List<UserDTO> employees = userService.getAllEmployees(manager);
                whatsAppService.sendMessage(userPhone, formatterService.formatEmployeeUpdateSelector(employees));
                if (!employees.isEmpty()) {
                    chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_UPDATE_SELECT_USER);
                } else {
                    chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_MENU_CHOICE);
                }
            }
            case "4" -> {
                List<UserDTO> employees = userService.getAllEmployees(manager);
                whatsAppService.sendMessage(userPhone, formatterService.formatEmployeeList(employees) + "\n\nDigite o e-mail do funcionário que deseja remover (ou 'V' para voltar):");
                if (!employees.isEmpty()) {
                    chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_REMOVE_EMAIL_CONFIRM);
                } else {
                    chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_MENU_CHOICE);
                }
            }
            case "V" -> {
                whatsAppService.sendMessage(userPhone, formatterService.formatMenu(manager.getProfile()));
                chatStateService.setState(userPhone, ChatState.AUTHENTICATED);
            }
            default -> {
                whatsAppService.sendMessage(userPhone, formatterService.formatFallbackError() + "\n\n" + formatterService.formatCrudMenu());
            }
        }
    }

    private void handleStateCrudAddName(String userPhone, String messageText) {
        chatStateService.saveData(userPhone, "crud_add_name", messageText.trim());
        whatsAppService.sendMessage(userPhone, "Qual é o e-mail do novo funcionário?");
        chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_ADD_EMAIL);
    }

    private void handleStateCrudAddEmail(String userPhone, String messageText) {
        chatStateService.saveData(userPhone, "crud_add_email", messageText.trim());
        whatsAppService.sendMessage(userPhone, "Qual é o telefone do novo funcionário? (Ex: 5579999998888)");
        chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_ADD_PHONE);
    }

    private void handleStateCrudAddPhone(String userPhone, String messageText) {
        try {
            String name = chatStateService.getData(userPhone, "crud_add_name", String.class).orElseThrow(() -> new NoSuchElementException("Nome não encontrado na sessão"));
            String email = chatStateService.getData(userPhone, "crud_add_email", String.class).orElseThrow(() -> new NoSuchElementException("Email não encontrado na sessão"));
            Long companyId = chatStateService.getData(userPhone, "crud_add_company_id", Long.class).orElseThrow(() -> new NoSuchElementException("ID da Empresa não encontrado na sessão"));
            String phone = messageText.trim();

            UserDTO newUserDTO = new UserDTO();
            newUserDTO.setName(name);
            newUserDTO.setEmail(email);
            newUserDTO.setPhone(phone);
            newUserDTO.setCompanyId(companyId);
            newUserDTO.setProfile(UserProfile.EMPLOYEE);

            User createdUser = userService.createUser(newUserDTO);

            whatsAppService.sendMessage(userPhone, formatterService.formatEmployeeAdded(new UserDTO(createdUser)));
            transitionToCrudPostAction(userPhone);

        } catch (DataIntegrityException e) {
            logger.warn("Erro de integridade ao criar usuário (e-mail duplicado) para {}: {}", userPhone, e.getMessage());
            whatsAppService.sendMessage(userPhone, "❌ " + e.getMessage() + "\n\nPor favor, digite um e-mail diferente:");
            chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_ADD_EMAIL);
        }
    }

    private void handleStateCrudRemoveEmailConfirm(String userPhone, String messageText) {
        User manager = getUserByPhone(userPhone);
        String emailToRemove = messageText.trim();

        if ("V".equalsIgnoreCase(emailToRemove)) {
            transitionToCrudMenu(userPhone, manager.getProfile());
            return;
        }

        try {
            User employee = userRepository.findByEmail(emailToRemove)
                    .orElseThrow(() -> new ResourceNotFoundException("Funcionário com e-mail '" + emailToRemove + "' não encontrado."));

            userService.deleteEmployee(employee.getId(), manager);

            whatsAppService.sendMessage(userPhone, formatterService.formatEmployeeRemoved(employee.getName()));
            transitionToCrudPostAction(userPhone);

        } catch (ResourceNotFoundException | AccessDeniedException e) {
            logger.warn("Falha ao remover funcionário para {}: {}", userPhone, e.getMessage());
            whatsAppService.sendMessage(userPhone, "❌ " + e.getMessage() + "\n\nTente novamente ou digite 'V' para voltar.");
        }
    }

    private void handleStateCrudUpdateSelectUser(String userPhone, String messageText) {
        User manager = getUserByPhone(userPhone);
        String emailToUpdate = messageText.trim();

        if ("V".equalsIgnoreCase(emailToUpdate)) {
            transitionToCrudMenu(userPhone, manager.getProfile());
            return;
        }

        try {
            User employee = userRepository.findByEmail(emailToUpdate)
                    .orElseThrow(() -> new ResourceNotFoundException("Funcionário com e-mail '" + emailToUpdate + "' não encontrado."));

            if (!employee.getCompany().getId().equals(manager.getCompany().getId())) {
                throw new AccessDeniedException("Este funcionário não pertence à sua empresa.");
            }

            UserDTO dto = new UserDTO(employee);
            chatStateService.saveData(userPhone, "crud_update_user_dto", dto);
            chatStateService.saveData(userPhone, "crud_update_user_id", employee.getId());

            whatsAppService.sendMessage(userPhone, formatterService.formatEmployeeUpdateFieldMenu(dto));
            chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_UPDATE_SELECT_FIELD);

        } catch (ResourceNotFoundException | AccessDeniedException e) {
            logger.warn("Falha ao selecionar funcionário para atualizar {}: {}", userPhone, e.getMessage());
            whatsAppService.sendMessage(userPhone, "❌ " + e.getMessage() + "\n\nDigite o e-mail correto ou 'V' para voltar.");
        }
    }

    private void handleStateCrudUpdateSelectField(String userPhone, String messageText) {
        String option = messageText.trim().toUpperCase();
        String fieldToUpdate;
        String prompt;

        try {
            switch (option) {
                case "1" -> {
                    fieldToUpdate = "name";
                    prompt = "Qual o novo *Nome*?";
                }
                case "2" -> {
                    fieldToUpdate = "email";
                    prompt = "Qual o novo *E-mail*?";
                }
                case "3" -> {
                    fieldToUpdate = "phone";
                    prompt = "Qual o novo *Telefone*? (Ex: 5579999998888)";
                }
                case "V" -> {
                    transitionToCrudMenu(userPhone, getUserProfile(userPhone));
                    return;
                }
                default -> {
                    UserDTO dto = chatStateService.getData(userPhone, "crud_update_user_dto", UserDTO.class)
                            .orElseThrow(() -> new NoSuchElementException("DTO do usuário para atualização não encontrado na sessão."));
                    whatsAppService.sendMessage(userPhone, formatterService.formatFallbackError() + "\n\n" +
                            formatterService.formatEmployeeUpdateFieldMenu(dto));
                    return;
                }
            }

            chatStateService.saveData(userPhone, "crud_update_field", fieldToUpdate);
            whatsAppService.sendMessage(userPhone, prompt);
            chatStateService.setState(userPhone, ChatState.AWAITING_CRUD_UPDATE_NEW_VALUE);

        } catch (NoSuchElementException e) {
            logger.error("Erro de sessão ao tentar atualizar campo: {}", e.getMessage());
            whatsAppService.sendMessage(userPhone, "😕 Ops, ocorreu um erro na sua sessão. Voltando ao menu de gerenciamento...");
            transitionToCrudMenu(userPhone, getUserProfile(userPhone));
        }
    }

    private void handleStateCrudUpdateNewValue(String userPhone, String messageText) {
        User manager = getUserByPhone(userPhone);
        String newValue = messageText.trim();

        try {
            UserDTO dto = chatStateService.getData(userPhone, "crud_update_user_dto", UserDTO.class)
                    .orElseThrow(() -> new NoSuchElementException("DTO do usuário para atualização não encontrado na sessão."));
            Long userId = chatStateService.getData(userPhone, "crud_update_user_id", Long.class)
                    .orElseThrow(() -> new NoSuchElementException("ID do usuário para atualização não encontrado na sessão."));
            String field = chatStateService.getData(userPhone, "crud_update_field", String.class)
                    .orElseThrow(() -> new NoSuchElementException("Campo para atualização não encontrado na sessão."));

            switch (field) {
                case "name" -> dto.setName(newValue);
                case "email" -> dto.setEmail(newValue);
                case "phone" -> dto.setPhone(newValue);
            }

            userService.updateUser(userId, dto, manager);

            whatsAppService.sendMessage(userPhone, "✅ " + field + " atualizado com sucesso para *" + newValue + "*.");
            transitionToCrudPostAction(userPhone);

        } catch (DataIntegrityException e) {
            logger.warn("Erro de integridade ao atualizar usuário para {}: {}", userPhone, e.getMessage());
            whatsAppService.sendMessage(userPhone, "❌ " + e.getMessage() + "\n\nPor favor, digite um valor diferente:");
        } catch (AccessDeniedException e) {
            logger.error("Erro de permissão inesperado ao atualizar {}: {}", userPhone, e.getMessage());
            whatsAppService.sendMessage(userPhone, "❌ Você não tem permissão para realizar esta ação.");
            transitionToCrudMenu(userPhone, manager.getProfile());
        } catch (NoSuchElementException e) {
            logger.error("Erro de sessão ao tentar salvar novo valor: {}", e.getMessage());
            whatsAppService.sendMessage(userPhone, "😕 Ops, ocorreu um erro na sua sessão. Voltando ao menu de gerenciamento...");
            transitionToCrudMenu(userPhone, manager.getProfile());
        }
    }

    private void handleStateCrudPostAction(String userPhone, String messageText) {
        UserProfile profile = getUserProfile(userPhone);
        String option = messageText.trim();

        switch (option) {
            case "1":
                transitionToCrudMenu(userPhone, profile);
                break;
            case "2":
                chatStateService.setState(userPhone, ChatState.AUTHENTICATED);
                whatsAppService.sendMessage(userPhone, formatterService.formatMenu(profile));
                break;
            case "3":
                chatStateService.clearAll(userPhone);
                whatsAppService.sendMessage(userPhone, "Atendimento encerrado. Obrigado por usar o DeneasyBot! 👋");
                break;
            default:
                whatsAppService.sendMessage(userPhone, formatterService.formatFallbackError() + "\n\n" + formatterService.formatCrudPostActionMenu());
                break;
        }
    }

    private void handleStateAwaitingFaqChoice(String userPhone, String messageText) {
        String option = messageText.trim();

        if ("V".equalsIgnoreCase(option)) {
            UserProfile profile = getUserProfile(userPhone);
            whatsAppService.sendMessage(userPhone, formatterService.formatMenu(profile));
            chatStateService.setState(userPhone, ChatState.AUTHENTICATED);
            return;
        }

        try {
            String answer = faqService.getFaqAnswer(option, userPhone);

            whatsAppService.sendMessage(userPhone, answer);
            chatStateService.setState(userPhone, ChatState.AWAITING_POST_ACTION);
            whatsAppService.sendMessage(userPhone, formatterService.formatPostActionMenu());

        } catch (IllegalArgumentException e) {
            logger.warn("Opção de FAQ inválida '{}' para usuário {}", option, userPhone);
            whatsAppService.sendMessage(userPhone, formatterService.formatFallbackError() + "\n\n" + faqService.getFaqMenu());

        } catch (Exception e) {
            logger.error("Erro ao processar resposta da FAQ {} para {}: {}", option, userPhone, e.getMessage(), e);
            whatsAppService.sendMessage(userPhone, "Desculpe, ocorreu um erro ao buscar essa informação. Por favor, tente novamente.\n\n" + faqService.getFaqMenu());
        }
    }

    private UserProfile getUserProfile(String userPhone) {
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado pelo telefone: " + userPhone + " (dentro de getUserProfile)"));

        return user.getProfile();
    }
}