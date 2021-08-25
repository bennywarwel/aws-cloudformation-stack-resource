FROM ghcr.io/graalvm/graalvm-ce:java11-21.2.0

ENV RESULT_DIR=/opt/workenv
ENV PATH=$PATH:${RESULT_DIR}/bin

### Install 
RUN microdnf install maven wget \
    && wget https://musl.libc.org/releases/musl-1.2.2.tar.gz \
    && tar -xvf musl-1.2.2.tar.gz \
    && pushd musl-1.2.2 \
    && ./configure --disable-shared --prefix=${RESULT_DIR} \
    && make \
    && make install \
    && export PATH=$PATH:${RESULT_DIR}/bin \
    && popd \
    && wget https://zlib.net/zlib-1.2.11.tar.gz \
    && tar -xvf zlib-1.2.11.tar.gz \
    && pushd zlib-1.2.11 \
    && export CC=musl-gcc \
    && ./configure --static --prefix=${RESULT_DIR} \
    && make \
    && make install \
    && popd \
    && cp usr/lib/gcc/x86_64-redhat-linux/8/libstdc++.a ${RESULT_DIR}/lib \
    && gu install native-image 


