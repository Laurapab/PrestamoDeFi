// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;


contract PrestamoDefi {
    address payable public socioPrincipal;

    struct Prestamo {
        uint256 id;
        address prestatario;
        uint256 monto;
        uint256 plazo;
        uint256 tiempoSolicitud;
        uint256 tiempoLimite;
        bool aprobado;
        bool reembolsado;
        bool liquidado;
    }

    struct Cliente {
        bool activado;
        uint256 saldoGarantia;
        mapping(uint256 => Prestamo) prestamos;
        uint256[] prestamoIds;
    }

    mapping(address => Cliente) public clientes;
    mapping(address => bool) public empleadosPrestamista;

    event SolicitudPrestamo(address prestatario, uint256 monto, uint256 plazo, uint256 tiempoSolicitud);
    event PrestamoAprobado(address prestatario, uint256 monto, uint256 id);
    event PrestamoReembolsado(address prestatario, uint256 monto, uint256 id);
    event GarantiaLiquidada(address prestatario, uint256 idPrestamo, uint256 montoLiquidado);
    
    modifier soloSocioPrincipal() {
        require(msg.sender == socioPrincipal, "Solo el socio principal puede realizar esta operacion");
        _;
    }
    
    modifier soloEmpleadoPrestamista() {
        require(empleadosPrestamista[msg.sender], "Solo los prestamistas pueden realizar esta operacion");
            _;
    }
    modifier soloClienteRegistrado() {
        require(clientes[msg.sender].activado, "Cliente no registrado o inactivo");
            _;
    }

    constructor() {
        socioPrincipal = payable(msg.sender);  
        empleadosPrestamista[socioPrincipal] = true;
    }

    function altaPrestamista(address nuevoPrestamista) public soloSocioPrincipal {
        require(!empleadosPrestamista[nuevoPrestamista], "El prestamista ya esta dado de alta.");
        empleadosPrestamista[nuevoPrestamista] = true;
    }

    function altaCliente(address nuevoCliente) public soloEmpleadoPrestamista {
        require(!clientes[nuevoCliente].activado, "El cliente ya esta dado de alta.");
        Cliente storage structNuevoCliente = clientes[nuevoCliente];
        structNuevoCliente.saldoGarantia = 0;
        structNuevoCliente.activado = true;
    }

    function depositarGarantia() external payable soloClienteRegistrado {
        clientes[msg.sender].saldoGarantia += msg.value;
    }

    function solicitarPrestamo(uint256 monto_, uint256 plazo_) public soloClienteRegistrado returns (uint256) {
        require(clientes[msg.sender].saldoGarantia >= monto_, "Saldo de garantia insuficiente");
        uint256 nuevoId = clientes[msg.sender].prestamoIds.length + 1;
    
        Prestamo storage nuevoPrestamo = clientes[msg.sender].prestamos[nuevoId];
        nuevoPrestamo.id = nuevoId;
        nuevoPrestamo.prestatario = msg.sender;
        nuevoPrestamo.monto = monto_;
        nuevoPrestamo.plazo = plazo_;
        nuevoPrestamo.tiempoSolicitud = block.timestamp;
        nuevoPrestamo.tiempoLimite = 0; 
        nuevoPrestamo.aprobado = false;
        nuevoPrestamo.reembolsado = false;
        nuevoPrestamo.liquidado = false;
        
        clientes[msg.sender].prestamoIds.push(nuevoId);
        
        emit SolicitudPrestamo(msg.sender, monto_, plazo_, nuevoPrestamo.tiempoSolicitud);
        
        return nuevoId;
    }


    function aprobarPrestamo(address prestatario_, uint256 id_) public soloEmpleadoPrestamista {
        Cliente storage prestatario = clientes[prestatario_];

        require(id_ > 0 && id_ <= prestatario.prestamoIds.length, "ID de prestamo no valido");

        Prestamo storage prestamo = prestatario.prestamos[id_];

        require(!prestamo.aprobado, "El prestamo ya esta aprobado");
        require(!prestamo.reembolsado, "El prestamo ya esta reembolsado");
        require(!prestamo.liquidado, "El prestamo ya esta liquidado");

        prestamo.aprobado = true;
        prestamo.tiempoLimite = block.timestamp + prestamo.plazo;
        emit PrestamoAprobado(prestatario_, prestamo.monto, id_);
    }

    function reembolsarPrestamo(uint256 id_) public soloClienteRegistrado {
        Cliente storage prestatario = clientes[msg.sender];

        require(id_ > 0 && id_ <= prestatario.prestamoIds.length, "ID de prestamo no valido");

        Prestamo storage prestamo = prestatario.prestamos[id_];

        require(prestamo.prestatario == msg.sender, "El prestamo no pertenece al prestatario actual");
        require(prestamo.aprobado, "El prestamo no esta aprobado");
        require(!prestamo.reembolsado, "El prestamo ya esta reembolsado");
        require(!prestamo.liquidado, "El prestamo ya esta liquidado");
        require(prestamo.tiempoLimite >= block.timestamp, "El tiempo limite del prestamo ha vencido");
        
        socioPrincipal.transfer(prestamo.monto);  
        prestamo.reembolsado = true;
        prestatario.saldoGarantia -= prestamo.monto;
        emit PrestamoReembolsado(msg.sender, prestamo.monto, id_);
    }

    function liquidarPrestamo(address prestatario_, uint256 id_) public soloEmpleadoPrestamista {
        Cliente storage prestatario = clientes[prestatario_];
        Prestamo storage prestamo = prestatario.prestamos[id_];
        
        require(prestamo.aprobado, "El prestamo no esta aprobado");
        require(!prestamo.reembolsado, "El prestamo ya fue reembolsado");
        require(!prestamo.liquidado, "El prestamo ya fue liquidado");
        require(block.timestamp > prestamo.tiempoLimite, "El tiempo limite no ha vencido");

        socioPrincipal.transfer(prestamo.monto);

        prestamo.liquidado = true;
        prestatario.saldoGarantia -= prestamo.monto;

        emit GarantiaLiquidada(prestatario_, id_, prestamo.monto);
    }

    
    function obtenerPrestamosPorPrestatario(address prestatario_) public view returns (uint256[] memory) {
        return clientes[prestatario_].prestamoIds;
    }


    function obtenerDetallesPrestamo(address prestatario_, uint256 id_) public view returns (Prestamo memory) {
        Prestamo memory detallePrestamo = clientes[prestatario_].prestamos[id_];
        return detallePrestamo;
    }   
}
