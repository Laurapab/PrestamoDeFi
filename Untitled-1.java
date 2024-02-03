// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;


contract PrestamoDefi {
    address payable public socioPrincipalAddress;

    struct prestamo {
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
    event GarantiaLiquidada(address prestatario, uint256 monto, uint256 id);


    modifier soloSocioPrincipal() {
    require(msg.sender == socioPrincipalAddress, "Solo el socio principal puede realizar esta operacion");
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
    socioPrincipal = msg.sender;
    
    constructor(address payable nuevaDireccionSocioPrincipal) {
    socioPrincipalAddress = nuevaDireccionSocioPrincipal;

    empleadosPrestamista[socioPrincipalAddress] = true;
}