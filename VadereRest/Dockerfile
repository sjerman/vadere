FROM eclipse-temurin:11.0.19_7-jre

ENV V_HOME /vadere

ENV HEAP 4096M

RUN addgroup vadere && adduser --gecos "" --disabled-password  --ingroup vadere --home $V_HOME vadere

USER vadere
WORKDIR $V_HOME

ENV SLEEP 0

# add directly the war
ADD target/*.jar $J_HOME/app.jar

VOLUME /tmp

EXPOSE 8080 8443

CMD echo "The application will start in ${SLEEP}s..." && \
    echo "Memory: ${HEAP}" && \
    sleep ${SLEEP} && \
    java -Xmx${HEAP} -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -jar $V_HOME/app.jar
