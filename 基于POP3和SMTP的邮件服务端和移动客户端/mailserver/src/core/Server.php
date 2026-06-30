<?php
require_once __DIR__ . '/../../config/constants.php';
require_once __DIR__ . '/Logger.php';

abstract class Server {
    protected $socket;
    protected $port;
    protected $host;
    protected $isRunning = false;
    protected $logger;
    
    public function __construct($host = '0.0.0.0', $port) {
        $this->host = $host;
        $this->port = $port;
        $this->logger = new Logger();
    }
    
    public function start() {
        $this->createSocket();
        $this->bindSocket();
        $this->listenSocket();
        
        $this->isRunning = true;
        $this->log("服务器启动在 {$this->host}:{$this->port}");
        
        $this->run();
    }
    
    protected function createSocket() {
        $this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
        if ($this->socket === false) {
            throw new Exception("无法创建socket: " . socket_strerror(socket_last_error()));
        }
        
        // 设置SO_REUSEADDR选项，避免"Address already in use"错误
        socket_set_option($this->socket, SOL_SOCKET, SO_REUSEADDR, 1);
    }
    
    protected function bindSocket() {
        if (!socket_bind($this->socket, $this->host, $this->port)) {
            throw new Exception("无法绑定socket: " . socket_strerror(socket_last_error($this->socket)));
        }
    }
    
    protected function listenSocket() {
        if (!socket_listen($this->socket, MAX_CONNECTIONS)) {
            throw new Exception("无法监听socket: " . socket_strerror(socket_last_error($this->socket)));
        }
    }
    
    protected function run() {
        $clients = [];
        
        while ($this->isRunning) {
            $read = array_merge([$this->socket], $clients);
            $write = $except = null;
            
            // 使用socket_select进行非阻塞监听
            if (socket_select($read, $write, $except, null) > 0) {
                // 处理新连接
                if (in_array($this->socket, $read)) {
                    $newClient = socket_accept($this->socket);
                    if ($newClient !== false) {
                        $clientId = (int)$newClient;
                        $clients[$clientId] = $newClient;
                        
                        $clientIp = '';
                        socket_getpeername($newClient, $clientIp);
                        $this->log("新客户端连接: {$clientIp}", LOG_INFO, $clientId);
                        
                        $this->onClientConnected($newClient, $clientId);
                    }
                    unset($read[array_search($this->socket, $read)]);
                }
                
                // 处理现有客户端的数据
                foreach ($read as $clientSocket) {
                    $clientId = (int)$clientSocket;
                    $data = socket_read($clientSocket, 2048, PHP_NORMAL_READ);
                    
                    if ($data === false || $data === '') {
                        // 客户端断开连接
                        $this->log("客户端断开连接", LOG_INFO, $clientId);
                        $this->onClientDisconnected($clientSocket, $clientId);
                        socket_close($clientSocket);
                        unset($clients[$clientId]);
                    } else {
                        // 处理客户端数据
                        $data = trim($data);
                        $this->log("收到数据: {$data}", LOG_DEBUG, $clientId);
                        $this->handleClientData($clientSocket, $clientId, $data);
                    }
                }
            }
            
            // 防止CPU占用过高
            usleep(10000); // 10ms
        }
    }
    
    public function stop() {
        $this->isRunning = false;
        if ($this->socket) {
            socket_close($this->socket);
        }
        $this->log("服务器已停止");
    }
    
    protected function log($message, $level = LOG_INFO, $clientId = null) {
        $prefix = $clientId ? "[Client {$clientId}] " : "[Server] ";
        $this->logger->log($prefix . $message, $level);
    }
    
    protected function sendResponse($socket, $response) {
        $response .= "\r\n";
        socket_write($socket, $response, strlen($response));
        $this->log("发送响应: " . trim($response), LOG_DEBUG, (int)$socket);
    }
    
    // 抽象方法，由子类实现
    abstract protected function onClientConnected($socket, $clientId);
    abstract protected function onClientDisconnected($socket, $clientId);
    abstract protected function handleClientData($socket, $clientId, $data);
}