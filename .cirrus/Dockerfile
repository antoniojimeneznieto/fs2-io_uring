FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.8.2_3.2.2

RUN apt-get update && apt-get install -y clang

RUN git clone https://github.com/axboe/liburing.git && \
    cd liburing && git checkout liburing-2.3 && \
    ./configure && make && sudo make install
