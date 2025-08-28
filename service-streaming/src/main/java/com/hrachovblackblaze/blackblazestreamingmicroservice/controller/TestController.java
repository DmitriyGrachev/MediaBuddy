package com.hrachovblackblaze.blackblazestreamingmicroservice.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
class TestController{

    private final S3Client s3;
    private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    private final S3AsyncClient s3AsyncClient;
    TestController(S3Client s3, S3AsyncClient s3AsyncClient) {
        this.s3 = s3;
        this.s3AsyncClient = s3AsyncClient;
    }
    /*
    @GetMapping(value = "stream1/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    Mono<ResponseEntity<Flux<DataBuffer>>> streamLocal(
            @PathVariable String filename,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {

        String range = rangeHeader == null ? "" : rangeHeader;

        Path filePath = Paths.get("src/main/resources/videoTestOld.mp4");
        long fileSize = Files.size(filePath);
        long start = 0;
        long end = fileSize - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            start = Long.parseLong(ranges[0]);
            if (ranges.length > 1 && !ranges[1].isEmpty()) {
                end = Long.parseLong(ranges[1]);
            }
        }

        long contentLength = end - start + 1;

        SeekableByteChannel channel = Files.newByteChannel(filePath, StandardOpenOption.READ);
        channel.position(start);

        Flux<DataBuffer> data = DataBufferUtils.read(channel, new DefaultDataBufferFactory(), 8192)
                .takeUntil(buffer -> {
                    long remaining = contentLength - buffer.readableByteCount();
                    contentLength -= buffer.readableByteCount();
                    return remaining <= 0;
                })
                .doFinally(signalType -> {
                    try {
                        channel.close();
                    } catch (IOException ignored) {}
                });

        return Mono.just(ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                .body(data));

    }

     */

    @GetMapping("/stream/{filename}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> stream(
            @PathVariable String filename,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        String range = (rangeHeader != null ? rangeHeader : "bytes=0-");
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket("media-buddy")
                .key("movies/" + filename)
                .range(range)
                .build();

        return Mono.fromCompletionStage(
                        s3AsyncClient.getObject(req, AsyncResponseTransformer.toPublisher())
                )
                .map(pub -> {
                    GetObjectResponse resp = pub.response();
                    // S3 отдаёт что-то вроде "bytes 1048576-2097151/12345678"
                    String contentRange = resp.contentRange();
                    long contentLength = resp.contentLength();

                    Flux<DataBuffer> flux = Flux.from(pub)
                            .map(bb -> bufferFactory.wrap(bb));

                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .header(HttpHeaders.CONTENT_RANGE, contentRange)
                            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                            .contentType(MediaType.parseMediaType(resp.contentType()))
                            .body(flux);
                });
    }

    @GetMapping(value = "/video/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Flux<DataBuffer>> streamVideo(@PathVariable String filename) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket("media-buddy")
                .key("movies/videoTestOld.mp4")
                .build();

        ResponseInputStream<GetObjectResponse> s3InputStream = s3.getObject(getObjectRequest);

        Flux<DataBuffer> body = DataBufferUtils.readInputStream(
                () -> s3InputStream,
                bufferFactory,
                64 * 1024 // chunk size
        );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }
    @GetMapping("/reactive/text")
    public Flux<byte[]> streamText() {
        return Flux.create(sink -> {
        try(InputStream in = Files.newInputStream(Path.of("src/main/resources/textfile"))){
            byte[] buffer = new byte[2];
            int bytesRead;
            while((bytesRead = in.read(buffer)) != -1){
                sink.next(Arrays.copyOf(buffer,bytesRead));
            }
            sink.complete();
        }catch (IOException e){
            e.printStackTrace();
            sink.error(e);
        }

        });
    }
    @GetMapping("/reactive/text1")
    public Mono<List<String>> streamText1() {
        Flux<String> flux = Flux.create(sink -> {
            try(BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream("src/main/resources/textfile")))){
                while(bf.ready()){
                    String line = bf.readLine();
                    if (line.length() > 3){
                        System.out.println(line);
                        sink.next(line);
                    }

                }
                sink.complete();
            }catch (IOException e){
                e.printStackTrace();
                sink.error(e);
            }
        });
        Mono<List<String>> mono = Mono.from(flux.collectList());
        return mono;
    }
    @GetMapping("/reactive/text2")
    public Mono<List<String>> streamText2() {
        //Path path = Paths.get("src/main/resources/textfile");

        Path path1 = Paths.get("src/main/resources/videoTestOld.mp4");
        long fileSize = path1.toFile().length();
        final long constSize = fileSize;
        System.out.println("Стартовый размер файла видео: " + fileSize);

        try(FileChannel channel = FileChannel.open(path1, StandardOpenOption.READ)){
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead;
            while ((bytesRead = channel.read(buffer)) != -1){
                fileSize =- bytesRead;
                System.out.println("Прочитано: " + bytesRead + " байт | Осталось: " + fileSize + "/" + constSize);
                buffer.clear();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return Mono.empty();

    }
}
/*
/* 2. Создаем presigner для генерации временного доступа
S3Presigner presigner = S3Presigner.builder()
        .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
        .region(Region.of(region))
        .endpointOverride(URI.create(endpointUrl))
        .build();

// 3. Строим запрос на доступ к объекту
GetObjectRequest getReq = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(remotePath)
        .build();

// 4. Генерируем временную ссылку (например, на 1 час)
GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofHours(1))
        .getObjectRequest(getReq)
        .build();
String presignedUrl = presigner.presignGetObject(presignReq)
        .url()
        .toString();

             */
/*
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/text")
    public ResponseEntity<String> getText(@RequestHeader(value = "Range",required = false) String range) {
        //Начальная строка и начало и конец по дефолту
        String str = "Trying out range header in test controller";
        int start = 0;
        int end = str.length() - 1;

        //Как я понял обычно запрос такого вида Range: bytes=10-100, 150-300 , .....
        //Пока что по обычному пример Range: bytes=10-30
        if(range != null && range.startsWith("bytes=")) {
            //Разбить на елементы
            String[] parts = range.replace("bytes=","").split("-");
            start = Integer.parseInt(parts[0]);
            if(parts.length >1 && !parts[1].isEmpty()) end = Integer.parseInt(parts[1]);
        }
        //Math.min(end + 1, str.length()) - используеться как я понял по тому что если запрос байтов end будет привышать весь обьем файла или в нашем случае строки то берем по максимуму что есть
        String partialResult = str.substring(start, Math.min(end + 1, str.length()));
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                //Заголовки ниже мне не понятны зачем их использовать , они типо кастомные и просто несут за собой информацию?
                .header("Content-Range", "bytes " + start + "-" + end + "/" + str.length())
                .header("Accept-Ranges", "bytes")
                .header("Content-Length", String.valueOf(partialResult.length()))
                .body(partialResult);
    }
    //Базовый стриминг не побайтово а криво 200 вместо 206
    @GetMapping("/streaming")
    public ResponseEntity<StreamingResponseBody> streamFile() throws IOException {
        //CPR - видимо какой-то интерфейс от Spring какторый обозначает путь к файлу и помечает как ресурс
        ClassPathResource cpr = new ClassPathResource("videoTestOld.mp4");
        //Открываем с ресурса поток чтения
        InputStream in = cpr.getInputStream();

        //StreamingResponseBody - отправляет данне частями по мере готовности при помощи outputStream - то биж он на прямую связан с потоком который ожидает ответа от этого контроллера
        StreamingResponseBody responseBody = outputStream -> {
            //Создадим буфер на 1024 в который будем записывать частями данные и потом отправлять их дальше
            byte[] buffer = new byte[1024];
            int bytesRead;
            //
            while ((bytesRead = in.read(buffer)) != -1) {
                //Записываем в ожидающий поток и сразу сбрасываем что бы произошла отправка
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }
            //Не забываем закрыть поток
            in.close();
        };

        // По суть обьект StreamingResponseBody - готов и нужно его вернуть, появились еще новые заголовки
         return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "inline; filename=videoTestOld.mp4")
                .body(responseBody);


    }
    @GetMapping("/streamingPartial")
    public ResponseEntity<StreamingResponseBody> streamFilePartial(@RequestHeader(value = "Range", required = false) String range) throws IOException {
        File file = new File("src/main/resources/videoTestOld.mp4");
        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1; // По умолчанию весь файл

        // Парсинг заголовка Range
        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.replace("bytes=", "").split("-");
            start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }
            // Проверка корректности диапазона
            if (start >= fileLength) {
                start = fileLength - 1;
            }
            if (end >= fileLength) {
                end = fileLength - 1;
            }
        }

        // Вычисляем длину запрошенного диапазона
        long contentLength = end - start + 1;

        // Создаём StreamingResponseBody
        long finalStart = start;
        StreamingResponseBody responseBody = outputStream -> {
            try (RandomAccessFile videoFile = new RandomAccessFile(file, "r")) {
                videoFile.seek(finalStart); // Перемещаем указатель на начало диапазона
                byte[] buffer = new byte[64 * 1024];
                //Сделать буфер поменьше например 100 для отладки
                long bytesToRead = contentLength; // Сколько байт осталось отправить

                while (bytesToRead > 0) {
                    int bytesToReadInBuffer = (int) Math.min(buffer.length, bytesToRead);
                    int bytesRead = videoFile.read(buffer, 0, bytesToReadInBuffer);
                    System.out.println("Sending " + bytesRead + " bytes...");
                    if (bytesRead == -1) {
                        break; // Конец файла
                    }
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                    bytesToRead -= bytesRead;
                }
            } // RandomAccessFile автоматически закрывается
        };

        // Формируем ответ
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header("Content-Range", "bytes " + start + "-" + end + "/" + fileLength)
                .header("Accept-Ranges", "bytes")
                .header("Content-Length", String.valueOf(contentLength))
                .header("Content-Disposition", "inline; filename=videoTestOld.mp4")
                .body(responseBody);
    }

    @GetMapping("/delayed")
    public DeferredResult<String> getDelayedResult(){
        DeferredResult<String> result = new DeferredResult<>();
        Executors.newSingleThreadScheduledExecutor().schedule(
                () -> result.setResult("Ответ через 5 секунд!"),
                5, TimeUnit.SECONDS
        );
        return result;

    }
    @GetMapping("/streamingUpgraded")
    public DeferredResult<ResponseEntity<StreamingResponseBody>> streamFileUpgraded(@RequestHeader(value = "Range", required = false) String range) throws IOException {
        DeferredResult<ResponseEntity<StreamingResponseBody>> deferredResult = new DeferredResult<>(TimeUnit.MINUTES.toMillis(5));
        File file = new File("src/main/resources/videoTestOld.mp4");
        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1; // По умолчанию весь файл

        // Парсинг заголовка Range
        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.replace("bytes=", "").split("-");
            start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }
            // Проверка корректности диапазона
            if (start >= fileLength) {
                start = fileLength - 1;
            }
            if (end >= fileLength) {
                end = fileLength - 1;
            }
        }

        // Вычисляем длину запрошенного диапазона
        long contentLength = end - start + 1;

        // Создаём StreamingResponseBody
        long finalStart = start;
        StreamingResponseBody responseBody = outputStream -> {
            try (RandomAccessFile videoFile = new RandomAccessFile(file, "r")) {
                videoFile.seek(finalStart); // Перемещаем указатель на начало диапазона
                byte[] buffer = new byte[1024];
                //Сделать буфер поменьше например 100 для отладки
                long bytesToRead = contentLength; // Сколько байт осталось отправить

                while (bytesToRead > 0) {
                    int bytesToReadInBuffer = (int) Math.min(buffer.length, bytesToRead);
                    int bytesRead = videoFile.read(buffer, 0, bytesToReadInBuffer);
                    System.out.println("Sending " + bytesRead + " bytes...");
                    if (bytesRead == -1) {
                        break; // Конец файла
                    }
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                    bytesToRead -= bytesRead;
                }
            } // RandomAccessFile автоматически закрывается
        };

        // Формируем ответ
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header("Content-Range", "bytes " + start + "-" + end + "/" + fileLength)
                .header("Accept-Ranges", "bytes")
                .header("Content-Length", String.valueOf(contentLength))
                .header("Content-Disposition", "inline; filename=videoTestOld.mp4")
                .header("Content-Type", "video/mp4"); // <<< ОБЯЗАТЕЛЬНО;
        ResponseEntity<StreamingResponseBody> responseEntity = bodyBuilder.body(responseBody);
        deferredResult.setResult(responseEntity);
        return deferredResult;
    }
    @GetMapping("/proxy")
    public void getProxyResult(HttpServletRequest request, HttpServletResponse response) {
        //берем с стандартного запроса заголовок , может быть null , интересно что используеться обычный HttpServletRequestResponse тоесть нетникаких аналогов Spring
        String range = request.getHeader("Range");
        //Создаем низкоуровневый интерфейс для сетевых запросов  , можно исползовать WebClient и тд , по хорошему инджектить как бин но в порядке теста подойдет
        RestTemplate restTemplate = new RestTemplate();
        //Как я понял у execute конструктор гду URI METHOD Request (CallBack - где идет настройка тела запрос , ПРО ЭТО ПОЖАЛЙСТА ПО ПОДЖРОБНЕЕ ), И RESPONSE (ResponseExtractor - про это тожа пожалуйста по подробнее)
        RequestCallback requestCallback = clientRequest -> {
            if (range != null) {
                clientRequest.getHeaders().add("Range", range);
            }
        };
        ResponseExtractor<Void> responseExtractor = serviceResponse ->{
            //PartialContent response status should be
          response.setStatus(serviceResponse.getStatusCode().value());
          //перебираем и добавляем все заголовки
            serviceResponse.getHeaders().forEach((name, values) -> {
                for (String value : values) {
                    response.addHeader(name, value);
                }
            });

          //Вот тут уже не понятно , мы типо открываем поток чтения с которого через StreamingresponseBody будут передавать байты через outPutStreamна streamingupgrade поинте?
            try(InputStream in = serviceResponse.getBody()) {
                //Просто копирую байты что бы не заморачиваться с массивами байтов , размер передачи настраиваетиься на микросервисе
                StreamUtils.copy(in, response.getOutputStream());
                //Записали байты в ответ на кклиенте и нужно зафлюшить но не закрывать
                response.getOutputStream().flush();
            }catch (IOException e) {
                //Тут ошибку отправляем , и только что понял что не факт что при запросе сразу прийдет ответ по тому что на тестовом микросервисе может быть задержка из - за чего возникнет ошибкк , возможно нужно подумать над использованием DrefferdResponse что бы дать знак что нужно возможно подаждать
                if (!response.isCommitted()) {
                    try {
                        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error during data streaming.");
                    } catch (IOException e1) {
                        System.out.println("Error during data streaming.");
                    }
                }
            }
            //ничего не возвращаем потому что нам важен факт записи
            return null;
        };
        restTemplate.execute(
                URI.create("http://localhost:8083/test/streamingUpgraded"),
                HttpMethod.GET,
                requestCallback,
                responseExtractor
                );

    }

}


 */