package http
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer

import java.util.concurrent.Executors


class SimpleHttpServer {
	int port
	private HttpServer server

	void start(int port = 8100, String webRootPath, Closure handler = {null}, Closure errorListener = {}) {
		this.port = port

		server = HttpServer.create(new InetSocketAddress(port), 0)
		server.createContext("/", new MyHandler(webRootPath, handler, errorListener))
		server.executor = Executors.newCachedThreadPool()
		server.start()
	}
	
	void stop() {
		if (server != null) server.stop(0)
	}

	private static class MyHandler implements HttpHandler {
		private final Closure handler
		private final Closure errorListener
		private final String webRootPath

		MyHandler(String webRootPath, Closure handler, Closure errorListener) {
			this.webRootPath = webRootPath
			this.handler = handler
			this.errorListener = errorListener
		}

		@Override void handle(HttpExchange exchange) {
			new Exchanger(exchange).with {
				try {
					def handlerResponse = this.handler(requestURI)
					if (handlerResponse != null) {
						replyWithText(handlerResponse.toString())
					} else if (requestURI.startsWith("/") && requestURI.size() > 1) {
						def file = new File(this.webRootPath + "${requestURI.toString()}")
						if (!file.exists()) {
							replyNotFound()
						} else {
							replyWithText(file.readLines().join("\n"), contentTypeOf(file))
						}
					} else {
						replyNotFound()
					}
				} catch (Exception e) {
					replyWithException(e)
					errorListener.call(e)
				}
			}
		}

		private static String contentTypeOf(File file) {
			if (file.name.endsWith(".css")) "text/css"
			else if (file.name.endsWith(".js")) "text/javascript"
			else if (file.name.endsWith(".html")) "text/html"
			else "text/plain"
		}

		private static class Exchanger {
			private final HttpExchange exchange

			Exchanger(HttpExchange exchange) {
				this.exchange = exchange
			}

			String getRequestURI() {
				exchange.requestURI.toString()
			}

			void replyWithText(String text, String contentType = "text/plain") {
				exchange.responseHeaders.set("Content-Type", contentType)
				exchange.sendResponseHeaders(200, 0)
				exchange.responseBody.write(text.bytes)
				exchange.responseBody.close()
			}

			void replyWithException(Exception e) {
				exchange.responseHeaders.set("Content-Type", "text/plain")
				exchange.sendResponseHeaders(500, 0)
				e.printStackTrace(new PrintStream(exchange.responseBody))
				exchange.responseBody.close()
			}

			void replyNotFound() {
				exchange.sendResponseHeaders(404, 0)
				exchange.responseBody.close()
			}
		}
	}
}
