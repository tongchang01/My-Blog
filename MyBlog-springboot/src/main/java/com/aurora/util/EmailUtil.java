package com.aurora.util;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import com.aurora.exception.BizException;
import com.aurora.model.dto.EmailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.PostConstruct;

@Component
public class EmailUtil {

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${aws.ses.accessKeyId}")
    private String accessKeyId;

    @Value("${aws.ses.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aws.ses.region}")
    private String region;

    @Value("${aws.ses.fromEmail}")
    private String fromEmail;

    @Value("${aws.ses.charset}")
    private String charset;

    // 1. 声明为成员变量，避免重复创建连接
    private AmazonSimpleEmailService sesClient;

    // 2. 初始化方法，Spring 容器启动后只执行一次
    @PostConstruct
    private void initSESClient() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, accessKeySecret);
        this.sesClient = AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }

    public void sendHtmlMail(EmailDTO emailDTO) {
        try {
            Context context = new Context();
            context.setVariables(emailDTO.getCommentMap());
            String htmlContent = templateEngine.process(emailDTO.getTemplate(), context);

            // 直接使用成员变量，不要再调用 getSESClient() 和 shutdown()
            Content subjectContent = new Content(emailDTO.getSubject()).withCharset(charset);
            Content htmlContentObj = new Content(htmlContent).withCharset(charset);

            Body body = new Body().withHtml(htmlContentObj);
            Message message = new Message().withSubject(subjectContent).withBody(body);
            Destination destination = new Destination().withToAddresses(emailDTO.getEmail());

            SendEmailRequest request = new SendEmailRequest()
                    .withSource(fromEmail)
                    .withDestination(destination)
                    .withMessage(message);

            sesClient.sendEmail(request);
            // 3. 删掉 sesClient.shutdown()，否则下次发邮件连接就断了
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("邮件发送失败：" + e.getMessage());
        }
    }
}


