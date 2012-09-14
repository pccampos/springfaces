package org.springframework.springfaces.simplebean;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SimpleBeanController {
	
	@RequestMapping("/view")
	public String start(Model model) {
		ConditionalBean conditionalBean = new ConditionalBean();
		conditionalBean.setFirstMessage("This message was set on Controller");
		model.addAttribute("bean", conditionalBean);
		return "view";
	}

}
