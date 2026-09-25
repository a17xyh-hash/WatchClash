package mihomo

import (
	C "github.com/metacubex/mihomo/constant"
	"github.com/metacubex/mihomo/hub/executor"

	_ "golang.org/x/mobile/bind"
)

var currentTun *inbound.Tun

// Start 由 Kotlin 调用（Java 类名 Mihomo）。
// homeDir    : App 的 filesDir（GeoIP.dat/GeoSite.dat 放这里）
// configPath : 配置文件路径（yaml）
// fd         : VpnService establish() 后拿到的 fd
// stack      : "gvisor" | "system" | "mixed"
// Start 以「纯代理模式」启动 mihomo 内核：不创建 TUN/VPN，完全依赖 config.yaml 的 mixed-port 监听本地端口。
// 适用于系统阉割了 VpnService 的设备（如三星 Galaxy Watch）。
// Start 以「纯代理模式」启动 mihomo 内核：不创建 TUN/VPN，完全依赖 config.yaml 的 mixed-port 监听本地端口。
// 适用于系统阉割了 VpnService 的设备（如三星 Galaxy Watch）。
func Start(homeDir string, configPath string, fd int, stack string) string {
	C.SetHomeDir(homeDir)
	cfg, err := executor.ParseWithPath(configPath)
	if err != nil {
		return "ParseWithPath: " + err.Error()
	}
	cfg.General.Tun.Enable = false
	executor.ApplyConfig(cfg, true)
	currentTun = nil
	return ""
}

// Stop 关闭隧道。
func Stop() string {
	if currentTun != nil {
		if err := currentTun.Close(); err != nil {
			currentTun = nil
			return err.Error()
		}
		currentTun = nil
	}
	return ""
}

// Version 返回内核版本，用于连通性自检。
func Version() string {
	return C.Version
}
