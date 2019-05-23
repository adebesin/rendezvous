FROM centos

# Configure MapR Repository
RUN printf "[maprtech] \n\
name=MapR Technologies \n\
baseurl=http://package.mapr.com/releases/v6.1.0/redhat/ \n\
enabled=1 \n\
gpgcheck=0 \n\
protect=1 \n\
 \n\
[maprecosystem] \n\
name=MapR Technologies \n\
baseurl=http://package.mapr.com/releases/MEP/MEP-6.1.0/redhat \n\
enabled=1 \n\
gpgcheck=0 \n\
protect=1" > /etc/yum.repos.d/maprtech.repo && \

# Install the mapr gpg key
rpm --import http://package.mapr.com/releases/pub/maprgpg.key && \

# Install Open JDK and MapR dependencies
yum install -y sudo java-1.8.0-openjdk net-tools mapr-client.x86_64 mapr-posix-client-basic && \

# Clean temp files
yum clean all

# Set the default User
ARG MAPR_CONTAINER_USER=mapr
ENV MAPR_CONTAINER_USER ${MAPR_CONTAINER_USER}
ARG MAPR_CONTAINER_UID=5000
ENV MAPR_CONTAINER_UID ${MAPR_CONTAINER_UID}

# Set the default Group
ARG MAPR_CONTAINER_GROUP=mapr
ENV MAPR_CONTAINER_GROUP ${MAPR_CONTAINER_GROUP}
ARG MAPR_CONTAINER_GID=5000
ENV MAPR_CONTAINER_GID ${MAPR_CONTAINER_GID}

#Add user
RUN groupadd -g $MAPR_CONTAINER_GID $MAPR_CONTAINER_GROUP && \
useradd -u $MAPR_CONTAINER_UID -g $MAPR_CONTAINER_GID $MAPR_CONTAINER_USER && \
usermod -a -G wheel $MAPR_CONTAINER_USER && \
echo '%wheel ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

USER $MAPR_CONTAINER_USER
CMD sudo /opt/mapr/server/configure.sh -N $MAPR_CLUSTER -C $MAPR_CLDB_HOSTS -Z $MAPR_ZOOKEEPER_HOSTS -c\
 -u $MAPR_CONTAINER_USER -g $MAPR_CONTAINER_GROUP && java -Djava.security.egd=file:/dev/./urandom -jar /app.jar
